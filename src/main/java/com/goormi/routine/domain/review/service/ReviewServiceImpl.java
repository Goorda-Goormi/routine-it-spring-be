package com.goormi.routine.domain.review.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.notification.entity.NotificationType;
import com.goormi.routine.domain.notification.service.NotificationService;
import com.goormi.routine.domain.ranking.repository.RankingRepository;
import com.goormi.routine.domain.ranking.service.RankingService;
import com.goormi.routine.domain.review.dto.MonthlyReviewResponse;
import com.goormi.routine.domain.review.repository.ReviewRedisRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import com.goormi.routine.domain.userActivity.entity.ActivityType;
import com.goormi.routine.domain.userActivity.entity.UserActivity;
import com.goormi.routine.domain.userActivity.repository.UserActivityRepository;
import com.goormi.routine.domain.personal_routines.domain.PersonalRoutine;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

	private final UserRepository userRepository;
	private final RankingService rankingService;
	private final NotificationService notificationService;
	private final GroupMemberRepository groupMemberRepository;
	private final ReviewRedisRepository reviewRedisRepository;
	private final RankingRepository rankingRepository;
	private final UserActivityRepository userActivityRepository;
	private final ObjectMapper objectMapper;
	private final AiReviewService aiReviewService;

	//동시성 제어
	@Qualifier("aiReviewExecutor")
	private final ExecutorService executorService;

	@Override
	public void sendMonthlyReviewMessages(String monthYear) {
		String targetMonth = monthYear != null ? monthYear :
			LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

		LocalDate startDate = LocalDate.parse(targetMonth + "-01");
		LocalDate endDate = startDate.plusMonths(1).minusDays(1);

		List<User> allUsers = userRepository.findAll();
		if (allUsers.isEmpty()) {
			log.warn("전송 대상 사용자가 없습니다.");
			return;
		}
		List<Long> allUserIds = allUsers.stream().map(User::getId).collect(Collectors.toList());

		// 1. ✨ 데이터 사전 로딩 (N+1 해결의 핵심)
		Map<Long, User> userMap = allUsers.stream().collect(Collectors.toMap(User::getId, user -> user));

		BatchData batchData = loadAllBatchData(allUserIds, startDate, endDate, targetMonth);

		Map<Long, Map<ActivityType, Integer>> allActivityCounts = batchData.getAllActivityCounts();
		Map<Long, Long> allScores = batchData.getAllScores();
		Map<Long, Integer> allActiveGroupCounts = batchData.getAllActiveGroupCounts();
		Map<Long, MonthlyReviewResponse> allPreviousReviews = batchData.getAllPreviousReviews();// ** ------------------------------------------- **

		// 2. 비동기 작업 실행 (루프 제거 및 Map 인자 전달)
		List<CompletableFuture<Boolean>> futures = allUsers.stream()
			.map(user -> sendUserReviewMessageAsync(
				user.getId(),
				targetMonth,
				userMap,
				allActivityCounts,
				allScores,
				allActiveGroupCounts,
				allPreviousReviews
			))
			.collect(Collectors.toList());
		int successCount = 0;
		int failCount = 0;

		try {
			// 모든 작업 완료 대기
			CompletableFuture<Void> allFutures =
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

			allFutures.get(30, TimeUnit.MINUTES);

			// 결과 수집
			for (CompletableFuture<Boolean> future : futures) {
				if (future.join()) {
					successCount++;
				} else {
					failCount++;
				}
			}

			log.info("전송 완료: 성공={}, 실패={}", successCount, failCount);

		} catch (TimeoutException e) {
			log.error("타임아웃 발생", e);
			throw new RuntimeException("처리 시간 초과", e);
		} catch (Exception e) {
			log.error("오류 발생", e);
			throw new RuntimeException();
		}

		if (failCount > 0) {
			throw new RuntimeException(String.format("일부 메시지 전송 실패: 성공 %d건, 실패 %d건", successCount, failCount));
		}
	}

	@Override
	public void sendUserReviewMessage(Long userId, String monthYear) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다.");
		}

		MonthlyReviewResponse currentReview = calculateMonthlyReview(userId, monthYear);

		String fallback = generateReviewMessage(currentReview);
		currentReview.setMessageContent(fallback);
		currentReview.setMessageSent(false);
		saveReviewToRedis(currentReview);

		notificationService.createNotification(
			NotificationType.MONTHLY_REVIEW,
			null,
			userId,
			null
		);

		// AI 메시지 생성 비동기로 분리
		generateAiMessageAsync(currentReview, userId, monthYear);

		log.info("월간 리뷰 기본 저장 및 알림 전송 완료 (AI 비동기 처리 예정) userId={}, month={}",
			userId, monthYear);
	}

	@Override
	public void sendReviewMessageBatch(
		Long userId,
		String monthYear,
		Map<Long, User> userMap,
		Map<Long, Map<ActivityType, Integer>> allActivityCounts,
		Map<Long, Long> allScores,
		Map<Long, Integer> allActiveGroupCounts,
		Map<Long, MonthlyReviewResponse> allPreviousReviews) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다.");
		}

		MonthlyReviewResponse currentReview = calculateMonthlyReviewBatch(
			userId,
			monthYear,
			userMap,
			allActivityCounts,
			allScores,
			allActiveGroupCounts,
			allPreviousReviews
		);

		String messageContent;
		try {
			//gemini 호출
			messageContent = generateAiMessageWithTimeout(currentReview, 10);
		} catch (TimeoutException e) {
			log.warn("API 타임아웃, 폴백 메시지 사용");
			messageContent = generateReviewMessage(currentReview);
		} catch (Exception e) {
			messageContent = generateReviewMessage(currentReview);
		}

		currentReview.setMessageContent(messageContent);
		currentReview.setMessageSent(true);

		saveReviewToRedis(currentReview);

		notificationService.createNotification(
			NotificationType.MONTHLY_REVIEW,
			null,
			userId,
			null);

		log.info("사용자 회고 메시지 전송 완료: 사용자 ID = {}, 월 = {}", userId, monthYear);

	}

	@Override
	public void retryFailedMessages(String monthYear) {
		String targetMonth = monthYear != null ? monthYear :
			LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));

		List<Long> failedUserIds = reviewRedisRepository.getFailedUserIds(targetMonth);

		if (failedUserIds.isEmpty()) {
			log.info("재전송할 실패 메시지가 없습니다. 월: {}", targetMonth);
			return;
		}

		int retrySuccessCount = 0;
		int retryFailCount = 0;

		for (Long userId : failedUserIds) {
			try {
				sendUserReviewMessage(userId, targetMonth);
				reviewRedisRepository.removeFailedMessage(userId, targetMonth);
				retrySuccessCount++;
			} catch (Exception e) {
				retryFailCount++;
				log.error("회고 메시지 재전송 실패: 사용자 ID = {}", userId, e);
			}
		}

		log.info("회고 메시지 재전송 완료: 월 = {}, 성공 = {}, 실패 = {}",
			targetMonth, retrySuccessCount, retryFailCount);
	}

	@Override
	public int getFailedMessageCount(String monthYear) {
		String targetMonth = monthYear != null ? monthYear :
			LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

		return reviewRedisRepository.getFailedMessageCount(targetMonth);
	}

	private MonthlyReviewResponse calculateMonthlyReview(Long userId, String monthYear) {
		LocalDate startDate = LocalDate.parse(monthYear + "-01");
		LocalDate endDate = startDate.plusMonths(1).minusDays(1);

		// List<UserActivity> monthlyActivities = userActivityRepository
		// 	.findByUserIdAndActivityDateBetween(userId, startDate, endDate);

		long personalRoutineCountLong = userActivityRepository
			.countByUserIdAndActivityTypeAndActivityDateBetween(
				userId, ActivityType.PERSONAL_ROUTINE_COMPLETE, startDate, endDate);
		int personalRoutineCount = (int)Math.min(personalRoutineCountLong, Integer.MAX_VALUE);

		long groupAuthCountLong = userActivityRepository
			.countByUserIdAndActivityTypeAndActivityDateBetween(
				userId, ActivityType.GROUP_AUTH_COMPLETE, startDate, endDate);
		int groupAuthCount = (int)Math.min(groupAuthCountLong, Integer.MAX_VALUE);

		long dailyChecklistCountLong = userActivityRepository
			.countByUserIdAndActivityTypeAndActivityDateBetween(
				userId, ActivityType.DAILY_CHECKLIST, startDate, endDate);
		int dailyChecklistCount = (int)Math.min(dailyChecklistCountLong, Integer.MAX_VALUE);

		int totalAuthCount = personalRoutineCount + groupAuthCount + dailyChecklistCount;

		try {
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

			long currentScore = rankingService.getTotalScoreByUser(userId);
			int currentGroups = groupMemberRepository.findActiveGroupsByUserId(userId).size();

			int personalRoutineAchievementRate = calculatePersonalRoutineAchievementRate(userId, monthYear);

			String previousMonth = getPreviousMonth(monthYear);
			MonthlyReviewResponse previousReview = null;
			if (previousMonth != null) {
				String previousData = reviewRedisRepository.getReviewData(userId.toString(), previousMonth);
				if (previousData != null) {
					previousReview = parseReviewData(previousData);
				}
			}

			int scoreDifference = 0;
			int groupDifference = 0;
			List<String> achievements = new ArrayList<>();

			if (previousReview != null) {
				scoreDifference = (int)currentScore - previousReview.getTotalScore();
				groupDifference = currentGroups - previousReview.getParticipatingGroups();

				if (scoreDifference > 0) {
					achievements.add(String.format("지난 달보다 %d점 향상! (%d → %d)",
						scoreDifference, previousReview.getTotalScore(), currentScore));
				}
				if (groupDifference > 0) {
					achievements.add(String.format("새로운 그룹 %d개 참여로 도전 영역 확장!", groupDifference));
				}
			} else {
				achievements.add("루틴잇 첫 달 도전 완료! 🎉");
				if (currentScore > 0) {
					achievements.add(String.format("첫 달 %d점 달성!", currentScore));
				}
			}

			return MonthlyReviewResponse.builder()
				.userId(userId)
				.nickname(user.getNickname())
				.monthYear(monthYear)
				.totalScore((int)currentScore)
				.participatingGroups(currentGroups)
				.personalRoutineAchievementRate(personalRoutineAchievementRate)
				.totalAuthCount(Math.max(totalAuthCount, 0))
				.personalRoutineCount(Math.max(personalRoutineCount, 0))
				.groupAuthCount(Math.max(groupAuthCount, 0))
				.dailyChecklistCount(Math.max(dailyChecklistCount, 0))
				.achievements(achievements)
				.scoreDifference(scoreDifference)
				.groupDifference(groupDifference)
				.createdAt(LocalDateTime.now())
				.build();

		} catch (Exception e) {
			log.error("월간 회고 계산 실패: 사용자 ID = {}, 월 = {}", userId, monthYear, e);
			throw new RuntimeException("회고 계산 중 오류가 발생했습니다.", e);
		}
	}

	private MonthlyReviewResponse calculateMonthlyReviewBatch(
		Long userId,
		String monthYear,
		Map<Long, User> userMap,
		Map<Long, Map<ActivityType, Integer>> allActivityCounts,
		Map<Long, Long> allScores,
		Map<Long, Integer> allActiveGroupCounts,
		Map<Long, MonthlyReviewResponse> allPreviousReviews
	) {
		LocalDate startDate = LocalDate.parse(monthYear + "-01");

		Map<ActivityType, Integer> activityCounts = allActivityCounts.getOrDefault(userId, Map.of());

		int personalRoutineCount = activityCounts.getOrDefault(ActivityType.PERSONAL_ROUTINE_COMPLETE, 0);
		int groupAuthCount = activityCounts.getOrDefault(ActivityType.GROUP_AUTH_COMPLETE, 0);
		int dailyChecklistCount = activityCounts.getOrDefault(ActivityType.DAILY_CHECKLIST, 0);

		int totalAuthCount = personalRoutineCount + groupAuthCount + dailyChecklistCount;

		try {
			User user = userMap.get(userId);
			if (user == null) {
				throw new IllegalArgumentException("사전 로딩된 사용자 데이터에서 찾을 수 없습니다: " + userId);
			}

			long currentScore = allScores.getOrDefault(userId, 0L);
			int currentGroups = allActiveGroupCounts.getOrDefault(userId, 0);

			int personalRoutineAchievementRate = calculatePersonalRoutineAchievementRate(userId, monthYear);

			String previousMonth = getPreviousMonth(monthYear);
			MonthlyReviewResponse previousReview = allPreviousReviews.get(userId); // ✨ Redis 배치 조회 결과 사용

			int scoreDifference = 0;
			int groupDifference = 0;
			List<String> achievements = new ArrayList<>();

			if (previousReview != null) {
				scoreDifference = (int)currentScore - previousReview.getTotalScore();
				groupDifference = currentGroups - previousReview.getParticipatingGroups();

				if (scoreDifference > 0) {
					achievements.add(String.format("지난 달보다 %d점 향상! (%d → %d)",
						scoreDifference, previousReview.getTotalScore(), currentScore));
				}
				if (groupDifference > 0) {
					achievements.add(String.format("새로운 그룹 %d개 참여로 도전 영역 확장!", groupDifference));
				}
			} else {
				achievements.add("루틴잇 첫 달 도전 완료! 🎉");
				if (currentScore > 0) {
					achievements.add(String.format("첫 달 %d점 달성!", currentScore));
				}
			}

			return MonthlyReviewResponse.builder()
				.userId(userId)
				.nickname(user.getNickname())
				.monthYear(monthYear)
				.totalScore((int)currentScore)
				.participatingGroups(currentGroups)
				.personalRoutineAchievementRate(personalRoutineAchievementRate)
				.totalAuthCount(Math.max(totalAuthCount, 0))
				.personalRoutineCount(Math.max(personalRoutineCount, 0))
				.groupAuthCount(Math.max(groupAuthCount, 0))
				.dailyChecklistCount(Math.max(dailyChecklistCount, 0))
				.achievements(achievements)
				.scoreDifference(scoreDifference)
				.groupDifference(groupDifference)
				.createdAt(LocalDateTime.now())
				.build();

		} catch (Exception e) {
			log.error("월간 회고 계산 실패: 사용자 ID = {}, 월 = {}", userId, monthYear, e);
			throw new RuntimeException("회고 계산 중 오류가 발생했습니다.", e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public MonthlyReviewResponse getMonthlyReview(Long userId, String monthYear) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다.");
		}

		String reviewData = reviewRedisRepository.getReviewData(userId.toString(), monthYear);

		if (reviewData == null) {
			// Redis에 데이터가 없으면 새로 계산해서 반환 (저장하지 않음)
			return calculateMonthlyReview(userId, monthYear);
		}

		try {
			return parseReviewData(reviewData);
		} catch (Exception e) {
			log.error("회고 데이터 파싱 실패. 새로 계산합니다. 사용자 ID: {}, 월: {}", userId, monthYear, e);
			return calculateMonthlyReview(userId, monthYear);
		}
	}

	private final int MAX_RETRIES = 3; // 최대 재시도 횟수
	private final long RETRY_DELAY_SECONDS = 2; // 재시도 간 딜레이(초)

	private void generateAiMessageAsync(MonthlyReviewResponse review, Long userId, String monthYear) {
		CompletableFuture.runAsync(() -> {
			int attempt = 0;
			while (attempt < MAX_RETRIES) {
				attempt++;

				if (attempt > 1) {
					try {
						log.info("AI 메시지 생성 재시도 대기 ({}초): userId={}, month={}, 시도={}",
							RETRY_DELAY_SECONDS, userId, monthYear, attempt);
						Thread.sleep(RETRY_DELAY_SECONDS * 1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						log.error("재시도 대기 중 스레드 인터럽트 발생: userId={}, month={}", userId, monthYear, e);
						break;
					}
				}

				try {
					String aiMessage = generateAiMessageWithTimeout(review, 10);

					// AI 메시지 생성 성공 시
					review.setMessageContent(aiMessage);
					review.setMessageSent(true);
					saveReviewToRedis(review);

					log.info("AI 메시지 생성 및 Redis 업데이트 성공: userId={}", userId);

				} catch (TimeoutException e) {
					log.warn("AI API 타임아웃 발생 (비동기): userId={}, month={}", userId, monthYear);

				} catch (Exception e) {
					log.error("AI 메시지 생성 중 예외 발생 (비동기): userId={}, month={}", userId, monthYear, e);
				}
				if (attempt == MAX_RETRIES) {
					log.error("AI 메시지 생성 최종 실패. 폴백 메시지 유지: userId={}, month={}, 총 시도={}",
						userId, monthYear, MAX_RETRIES);
				}
			}
		}, executorService);
	}

	private int calculatePersonalRoutineAchievementRate(Long userId, String monthYear) {
		try {
			LocalDate startDate = LocalDate.parse(monthYear + "-01");
			LocalDate endDate = startDate.plusMonths(1).minusDays(1);

			List<UserActivity> personalRoutineActivities = userActivityRepository
				.findByUserIdAndActivityTypeAndActivityDateBetween(
					userId,
					ActivityType.PERSONAL_ROUTINE_COMPLETE,
					startDate,
					endDate
				);

			if (personalRoutineActivities.isEmpty()) {
				return 0;
			}

			Map<Long, List<UserActivity>> activitiesByRoutine = personalRoutineActivities.stream()
				.filter(activity -> activity.getPersonalRoutine() != null)
				.collect(
					Collectors.groupingBy(activity -> activity.getPersonalRoutine().getRoutineId().longValue()));

			if (activitiesByRoutine.isEmpty()) {
				return 0;
			}

			List<Double> achievementRates = new ArrayList<>();

			for (Map.Entry<Long, List<UserActivity>> entry : activitiesByRoutine.entrySet()) {
				List<UserActivity> activities = entry.getValue();

				PersonalRoutine routine = activities.get(0).getPersonalRoutine();
				int targetCount = calculateMonthlyTargetCount(routine, startDate, endDate);

				if (targetCount > 0) {
					double achievementRate = Math.min(100.0, (double)activities.size() / targetCount * 100);
					achievementRates.add(achievementRate);
				}
			}

			return achievementRates.isEmpty() ? 0 :
				(int)achievementRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

		} catch (Exception e) {
			log.warn("개인 루틴 성취률 계산 실패: 사용자 ID = {}", userId, e);
			return 0;
		}
	}

	private int calculateMonthlyTargetCount(PersonalRoutine routine, LocalDate monthStart, LocalDate monthEnd) {
		try {
			String repeatDays = routine.getRepeatDays();
			if (repeatDays == null || repeatDays.length() != 7) {
				return 0;
			}

			LocalDate current = monthStart;
			int targetCount = 0;

			while (!current.isAfter(monthEnd)) {
				int dayOfWeek = current.getDayOfWeek().getValue();
				int repeatIndex = dayOfWeek == 7 ? 0 : dayOfWeek;

				if (repeatIndex < repeatDays.length() && repeatDays.charAt(repeatIndex) == '1') {
					targetCount++;
				}
				current = current.plusDays(1);
			}

			return targetCount;
		} catch (Exception e) {
			log.warn("월간 목표 횟수 계산 실패: 루틴 ID = {}", routine.getRoutineId(), e);
			return 0;
		}
	}

	private void saveReviewToRedis(MonthlyReviewResponse review) {
		try {
			String jsonData = objectMapper.writeValueAsString(review);
			reviewRedisRepository.saveReviewData(review.getUserId().toString(), review.getMonthYear(),
				jsonData); // 변경된 부분
		} catch (JsonProcessingException e) {
			log.error("회고 데이터 JSON 변환 실패: 사용자 ID = {}, 월 = {}",
				review.getUserId(), review.getMonthYear(), e);
			throw new RuntimeException("회고 데이터 저장 실패", e);
		}
	}

	private MonthlyReviewResponse parseReviewData(String jsonData) {
		try {
			return objectMapper.readValue(jsonData, MonthlyReviewResponse.class);
		} catch (JsonProcessingException e) {
			log.error("회고 데이터 JSON 파싱 실패", e);
			return null;
		}
	}

	private String getPreviousMonth(String monthYear) {
		try {
			LocalDate date = LocalDate.parse(monthYear + "-01");
			return date.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
		} catch (Exception e) {
			return null;
		}
	}

	//ai 호출 실패 시
	private String generateReviewMessage(MonthlyReviewResponse review) {
		StringBuilder message = new StringBuilder();

		message.append("🎊 ").append(review.getMonthYear()).append(" 월간 루틴 성과 리포트 🎊\n\n");
		message.append("안녕하세요, ").append(review.getNickname()).append("님!\n");
		message.append("이번 달 루틴 성과를 확인해보세요.\n\n");

		message.append("📈 이번 달 성과\n");
		message.append("• 총 점수: ").append(review.getTotalScore()).append("점");

		if (review.getScoreDifference() != null) {
			if (review.getScoreDifference() > 0) {
				message.append(" (📈 +").append(review.getScoreDifference()).append("점 상승!)");
			} else if (review.getScoreDifference() < 0) {
				message.append(" (📉 ").append(review.getScoreDifference()).append("점 하락)");
			} else {
				message.append(" (➡️ 동일)");
			}
		}
		message.append("\n");

		message.append("• 참여 그룹: ").append(review.getParticipatingGroups()).append("개");
		if (review.getGroupDifference() != null && review.getGroupDifference() > 0) {
			message.append(" (👥 +").append(review.getGroupDifference()).append("개 그룹 추가!)");
		}
		message.append("\n");

		message.append("• 총 인증: ")
			.append(review.getTotalAuthCount() != null ? review.getTotalAuthCount() : 0)
			.append("회\n");

		message.append("📊 활동별 상세 현황\n");
		int personalCount = review.getPersonalRoutineCount() != null ? review.getPersonalRoutineCount() : 0;
		message.append("🎯 개인 루틴: ").append(personalCount).append("회");
		if (personalCount > 0 && review.getPersonalRoutineAchievementRate() != null) {
			message.append(" (달성률 ").append(review.getPersonalRoutineAchievementRate()).append("%)");
		}
		message.append("\n");

		int groupCount = review.getGroupAuthCount() != null ? review.getGroupAuthCount() : 0;
		message.append("👥 그룹 인증: ").append(groupCount).append("회\n");

		int checklistCount = review.getDailyChecklistCount() != null ? review.getDailyChecklistCount() : 0;
		message.append("✅ 출석 체크: ").append(checklistCount).append("회\n");
		message.append("• 참여 그룹: ").append(review.getParticipatingGroups()).append("개\n\n");

		if (review.getPersonalRoutineAchievementRate() != null) {
			if (review.getPersonalRoutineAchievementRate() >= 90) {
				message.append("🎉 개인 루틴 90% 이상 달성! 완벽한 한 달이었어요!\n\n");
			} else if (review.getPersonalRoutineAchievementRate() >= 80) {
				message.append("⭐ 개인 루틴 80% 이상! 정말 훌륭한 실천력이에요!\n\n");
			} else if (review.getPersonalRoutineAchievementRate() >= 70) {
				message.append("💪 개인 루틴 70% 달성! 꾸준함이 보여요!\n\n");
			} else if (review.getPersonalRoutineAchievementRate() >= 50) {
				message.append("🌟 개인 루틴 절반 이상 달성! 다음 달은 더 높여보세요!\n\n");
			} else {
				message.append("💪 개인 루틴에 더 집중해보세요! 작은 시작이 큰 변화를 만듭니다!\n\n");
			}
		} else {
			message.append("🎯 다음 달에는 개인 루틴도 도전해보세요!\n\n");
		}

		if (review.getScoreDifference() != null && review.getScoreDifference() > 0) {
			message.append("💪 지난 달보다 더 발전한 모습이 보여요! 이 기세로 쭉~\n\n");
		} else {
			message.append("💪 꾸준함이 최고의 재능입니다! 다음 달도 화이팅!\n\n");
		}

		message.append("새로운 달에도 함께 성장해요! 🌱\n");
		message.append("루틴잇에서 확인하기 👆");

		return message.toString();
	}

	private CompletableFuture<Boolean> sendUserReviewMessageAsync(
		Long userId,
		String monthYear,
		Map<Long, User> userMap,
		Map<Long, Map<ActivityType, Integer>> allActivityCounts,
		Map<Long, Long> allScores,
		Map<Long, Integer> allActiveGroupCounts,
		Map<Long, MonthlyReviewResponse> allPreviousReviews) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				sendReviewMessageBatch(
					userId,
					monthYear,
					userMap,
					allActivityCounts,
					allScores,
					allActiveGroupCounts,
					allPreviousReviews);
				return true;
			} catch (Exception e) {
				log.error("전송 실패: userId={}", userId, e);
				reviewRedisRepository.saveFailedMessage(userId, monthYear, e.getMessage());
				return false;
			}
		}, executorService);
	}

	private String generateAiMessageWithTimeout(
		MonthlyReviewResponse review, long timeoutSeconds)
		throws TimeoutException, Exception {

		Future<String> future = executorService.submit(() -> {
			try {
				return aiReviewService.generateAiMessage(review);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		try {
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw e;
		}
	}

	@RequiredArgsConstructor
	@Getter
	private static class BatchData {
		private final Map<Long, Map<ActivityType, Integer>> allActivityCounts;
		private final Map<Long, Long> allScores;
		private final Map<Long, Integer> allActiveGroupCounts;
		private final Map<Long, MonthlyReviewResponse> allPreviousReviews;
	}

	private BatchData loadAllBatchData(List<Long> allUserIds, LocalDate startDate, LocalDate endDate, String targetMonth) {

		// 1. 활동 카운트 배치 조회 및 변환
		List<Object[]> activityResults = userActivityRepository.countActivitiesBatch(allUserIds, startDate, endDate);
		Map<Long, Map<ActivityType, Integer>> allActivityCounts = activityResults.stream()
			.collect(Collectors.groupingBy(
				result -> (Long) result[0],
				Collectors.toMap(
					result -> (ActivityType) result[1],
					result -> (Integer) result[2]
				)
			));

		// 2. 점수 배치 조회 및 변환
		List<Object[]> scoreResults = rankingRepository.findTotalScoresByUserIds(allUserIds);

		Map<Long, Long> allScores = scoreResults.stream()
			.collect(Collectors.toMap(
				result -> (Long) result[0],  // Key: User ID
				result -> (Long) result[1]   // Value: Total Score
			));

		// 3. 그룹 카운트 배치 조회 및 변환
		List<Object[]> groupResults = groupMemberRepository.countActiveGroupsBatch(allUserIds);

		Map<Long, Integer> allActiveGroupCounts = groupResults.stream()
			.collect(Collectors.toMap(
				result -> (Long) result[0],    // Key: User ID
				result -> (Integer) result[1]  // Value: Active Group Count
			));

		// 4. 이전 회고 데이터 배치 조회 (Redis)
		String previousMonth = getPreviousMonth(targetMonth);
		Map<Long, String> previousReviewsJsonMap = reviewRedisRepository.getPreviousReviewsJsonBatch(allUserIds, previousMonth);

		Map<Long, MonthlyReviewResponse> allPreviousReviews = previousReviewsJsonMap.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> parseReviewData(entry.getValue())
			));

		allPreviousReviews.values().removeIf(java.util.Objects::isNull);


		return new BatchData(
			allActivityCounts,
			allScores,
			allActiveGroupCounts,
			allPreviousReviews
		);
	}

}