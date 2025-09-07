package com.goormi.routine.domain.ranking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.ranking.dto.GlobalGroupRankingResponse;
import com.goormi.routine.domain.ranking.dto.GroupTop3RankingResponse;
import com.goormi.routine.domain.ranking.dto.PersonalRankingResponse;
import com.goormi.routine.domain.ranking.entity.Ranking;
import com.goormi.routine.domain.ranking.repository.RankingRedisRepository;
import com.goormi.routine.domain.ranking.repository.RankingRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import com.goormi.routine.domain.userActivity.entity.ActivityType;
import com.goormi.routine.domain.userActivity.entity.UserActivity;
import com.goormi.routine.domain.userActivity.repository.UserActivityRepository;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RankingServiceImpl implements RankingService {

	private final UserRepository userRepository;
	private final RankingRepository rankingRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final RankingRedisRepository rankingRedisRepository;
	private final UserActivityRepository userActivityRepository;

	@Override
	public List<PersonalRankingResponse> getPersonalRankings() {
		String currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
		List<Ranking> allGroupRankings = rankingRepository.findGroupRankingsOrderByScore();

		Map<Long, Integer> userTotalScores = allGroupRankings.stream()
			.collect(Collectors.groupingBy(
				Ranking::getUserId,
				Collectors.summingInt(ranking -> {
					int baseScore = ranking.getScore();

					int consecutiveDays = calculateConsecutiveDays(ranking.getUserId());
					double consecutiveBonus = calculateConsecutiveBonus(consecutiveDays);

					return baseScore + (int)consecutiveBonus;
				})
			));

		List<Map.Entry<Long, Integer>> sortedUsers = userTotalScores.entrySet().stream()
			.sorted((a, b) -> b.getValue().compareTo(a.getValue()))
			.collect(Collectors.toList());

		return IntStream.range(0, sortedUsers.size())
			.mapToObj(index -> {
				Map.Entry<Long, Integer> entry = sortedUsers.get(index);
				Long userId = entry.getKey();
				Integer totalScore = entry.getValue();

				User user = userRepository.findById(userId).orElse(null);

				return PersonalRankingResponse.builder()
					.currentRank(index + 1)
					.userId(userId)
					.nickname(user != null ? user.getNickname() : "탈퇴한 사용자")
					.totalScore(totalScore)
					.totalParticipants(sortedUsers.size())
					.monthYear(currentMonthYear)
					.consecutiveDays(calculateConsecutiveDays(userId))
					.groupDetails(getGroupDetailsByUserId(userId, currentMonthYear))
					.updatedAt(LocalDateTime.now())
					.build();
			})
			.collect(Collectors.toList());
	}

	@Override
	public List<GlobalGroupRankingResponse> getGlobalGroupRankings() {
		String monthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

		List<Ranking> groupRankings = rankingRepository.findGroupRankingsOrderByScore();

		List<GroupScoreData> groupScoreList = groupRankings.stream()
			.map(ranking -> {
				Long groupId = ranking.getGroupId();
				Group group = ranking.getGroup();

				int membersTotalScore = calculateGroupMembersTotalScore(groupId);

				int participationBonus = calculateSimpleParticipationBonus(groupId, monthYear);

				int finalScore = membersTotalScore + participationBonus;

				return new GroupScoreData(groupId, group, finalScore, membersTotalScore, participationBonus);
			})
			.sorted((a, b) -> Integer.compare(b.getFinalScore(), a.getFinalScore())) // 점수 내림차순 정렬
			.collect(Collectors.toList());

		List<GlobalGroupRankingResponse.GroupRankingItem> rankingItems =
			IntStream.range(0, groupScoreList.size())
				.mapToObj(index -> {
					GroupScoreData scoreData = groupScoreList.get(index);
					Long groupId = scoreData.getGroupId();
					Group group = scoreData.getGroup();

					int memberCount = groupMemberRepository.countMembersByGroupId(groupId);
					int activeMembers = groupMemberRepository.countActiveByGroupId(groupId, monthYear);
					int totalAuthCount = groupMemberRepository.countAuthByGroupId(groupId, monthYear);

					double participationRate = memberCount > 0 ? (double)activeMembers / memberCount : 0.0;
					double averageAuthPerMember = memberCount > 0 ? (double)totalAuthCount / memberCount : 0.0;

					return GlobalGroupRankingResponse.GroupRankingItem.builder()
						.rank(index + 1) // 새로운 순위
						.groupId(groupId)
						.groupName(group != null ? group.getGroupName() : "삭제된 그룹")
						.groupImageUrl(group != null ? group.getGroupImageUrl() : null)
						.category(group != null ? group.getCategory() : null)
						.groupType(group != null ? group.getGroupType().name() : null)
						.totalScore(scoreData.getFinalScore())
						.memberCount(memberCount)
						.activeMembers(activeMembers)
						.participationRate(Math.round(participationRate * 100.0) / 100.0)
						.totalAuthCount(totalAuthCount)
						.averageAuthPerMember(Math.round(averageAuthPerMember * 100.0) / 100.0)
						.build();
				})
				.collect(Collectors.toList());

		GlobalGroupRankingResponse response = GlobalGroupRankingResponse.builder()
			.rankings(rankingItems)
			.monthYear(monthYear)
			.totalGroups(rankingItems.size())
			.updatedAt(LocalDateTime.now())
			.build();

		return Collections.singletonList(response);
	}

	@Override
	public GroupTop3RankingResponse getTop3RankingsByGroup(Long groupId) {
		if (groupId == null) {
			throw new IllegalArgumentException("그룹 ID는 필수입니다.");
		}

		List<Ranking> top3Rankings = rankingRepository.findTop3UsersByGroupId(groupId);
		String currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

		if (top3Rankings.isEmpty()) {
			return GroupTop3RankingResponse.builder()
				.groupId(groupId)
				.top3Users(Collections.emptyList())
				.totalMembers(0)
				.monthYear(currentMonthYear)
				.updatedAt(LocalDateTime.now())
				.build();
		}

		Group group = top3Rankings.get(0).getGroup();
		double groupWeightMultiplier = getGroupWeightMultiplier(group);

		List<GroupTop3RankingResponse.UserRankingItem> userRankingItems =
			IntStream.range(0, top3Rankings.size())
				.mapToObj(index -> {
					Ranking ranking = top3Rankings.get(index);
					User user = ranking.getUser();

					int authCount = calculateGroupAuthCount(ranking.getUserId(), groupId, currentMonthYear);
					int consecutiveDays = calculateConsecutiveDays(ranking.getUserId());
					double consecutiveBonus = calculateConsecutiveBonus(consecutiveDays);

					GroupTop3RankingResponse.ScoreBreakdown scoreBreakdown =
						GroupTop3RankingResponse.ScoreBreakdown.builder()
							.baseScore(authCount * 10)
							.weightMultiplier(groupWeightMultiplier)
							.weightedScore((int)(authCount * 10 * groupWeightMultiplier))
							.consecutiveBonus(consecutiveBonus)
							.finalScore(ranking.getScore())
							.build();

					return GroupTop3RankingResponse.UserRankingItem.builder()
						.rank(index + 1)
						.userId(ranking.getUserId())
						.nickname(user != null ? user.getNickname() : "탈퇴한 사용자")
						.profileImageUrl(user != null ? user.getProfileImageUrl() : null)
						.score(ranking.getScore())
						.authCount(authCount)
						.consecutiveDays(consecutiveDays)
						.consecutiveBonus(consecutiveBonus)
						.scoreBreakdown(scoreBreakdown)
						.build();
				})
				.collect(Collectors.toList());

		return GroupTop3RankingResponse.builder()
			.groupId(groupId)
			.groupName(group != null ? group.getGroupName() : "삭제된 그룹")
			.groupType(group != null ? group.getGroupType().name() : null)
			.groupWeightMultiplier(groupWeightMultiplier)
			.monthYear(currentMonthYear)
			.top3Users(userRankingItems)
			.totalMembers(groupMemberRepository.countMembersByGroupId(groupId))
			.updatedAt(LocalDateTime.now())
			.build();
	}

	@Override
	@Transactional
	public void updateRankingScore(Long userId, Long groupId, int score) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다.");
		}
		if (score < 0) {
			throw new IllegalArgumentException("점수는 0 이상이어야 합니다.");
		}

		if (groupId != null) {
			int baseScore = score * 10;
			updateGroupScore(userId, groupId, baseScore);
		} else {
			updatePersonalScore(userId, score);
		}
	}

	@Override
	@Transactional
	public void updatePersonalScore(Long userId, int score) {
		Optional<Ranking> existingRanking = rankingRepository.findByUserIdAndGroupId(userId, null);

		if (existingRanking.isPresent()) {
			Ranking ranking = existingRanking.get();
			ranking.setScore(ranking.getScore() + score);
			ranking.setUpdatedAt(LocalDateTime.now());
			rankingRepository.save(ranking);
			log.info("개인 점수 업데이트: 사용자 ID = {}, 추가 점수 = {}, 총 점수 = {}",
				userId, score, ranking.getScore());
		} else {
			initializeRanking(userId, null);
			updatePersonalScore(userId, score);
		}
	}

	@Override
	@Transactional
	public void updateGroupScore(Long userId, Long groupId, int authCount) {
		int baseScore = authCount * 10;

		Optional<Ranking> existingRanking = rankingRepository.findByUserIdAndGroupId(userId, groupId);

		if (existingRanking.isPresent()) {
			Ranking ranking = existingRanking.get();
			ranking.setScore(ranking.getScore() + baseScore);
			ranking.setUpdatedAt(LocalDateTime.now());
			rankingRepository.save(ranking);
			log.info("그룹 점수 업데이트: 사용자 ID = {}, 그룹 ID = {},인증 횟수 = {}, 기본 점수 = {}, 총 점수 = {}",
				userId, groupId, authCount, baseScore, ranking.getScore());
		} else {
			initializeRanking(userId, groupId);
			updateGroupScore(userId, groupId, authCount);
		}
	}

	@Override
	@Transactional
	public void resetMonthlyRankings() {
		String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

		List<Ranking> allRankings = rankingRepository.findAll();
		if (allRankings.isEmpty()) {
			log.warn("리셋할 랭킹 데이터가 없습니다.");
			return;
		}

		for (Ranking ranking : allRankings) {
			ranking.setScore(0);
			ranking.setMonthYear(currentMonth);
			ranking.setUpdatedAt(LocalDateTime.now());
		}

		rankingRepository.saveAll(allRankings);
		rankingRedisRepository.saveLastResetMonth(currentMonth);

		log.info("월별 랭킹 리셋 완료: 총 {} 개의 랭킹이 리셋되었습니다", allRankings.size());

	}

	@Override
	public long getTotalScoreByUser(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다.");
		}

		return rankingRepository.findAll().stream()
			.filter(ranking -> userId.equals(ranking.getUserId()) && ranking.getGroupId() != null)
			.mapToInt(ranking -> {
				int baseScore = ranking.getScore();

				int consecutiveDays = calculateConsecutiveDays(ranking.getUserId());
				double consecutiveBonus = calculateConsecutiveBonus(consecutiveDays);

				return baseScore + (int)consecutiveBonus;
			})
			.sum();}

	@Override
	@Transactional
	public void initializeRanking(Long userId, Long groupId) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다.");
		}

		String currentMonthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
		Optional<Ranking> existingRanking = rankingRepository.findByUserIdAndGroupId(userId, groupId);

		if (existingRanking.isEmpty()) {
			Long rankingId = System.currentTimeMillis();

			Ranking newRanking = Ranking.builder()
				.rankingId(rankingId)
				.userId(userId)
				.groupId(groupId)
				.score(0)
				.monthYear(currentMonthYear)
				.updatedAt(LocalDateTime.now())
				.build();

			rankingRepository.save(newRanking);
			log.info("새로운 랭킹 초기화: 사용자 ID = {}, 그룹 ID = {}, 월 = {}",
				userId, groupId, currentMonthYear);
		}
	}


	private int calculateConsecutiveDays(Long userId) {
		try {
			List<UserActivity> activities = userActivityRepository
				.findByUserIdAndActivityTypeOrderByCreatedAtDesc(userId, ActivityType.GROUP_AUTH_COMPLETE);

			if (activities.isEmpty()) {
				return 0;
			}

			int consecutiveDays = 0;
			LocalDate currentDate = LocalDate.now();

			for (UserActivity activity : activities) {
				LocalDate activityDate = activity.getCreatedAt().toLocalDate();
				if (activityDate.equals(currentDate.minusDays(consecutiveDays))) {
					consecutiveDays++;
				} else {
					break;
				}
			}

			return consecutiveDays;
		} catch (Exception e) {
			log.warn("연속 일수 계산 실패: 사용자 ID = {}", userId, e);
			return 0;
		}
	}

	private List<PersonalRankingResponse.GroupRankingDetail> getGroupDetailsByUserId(Long userId,
		String monthYear) {
		try {
			List<GroupMember> activeGroups = groupMemberRepository.findActiveGroupsByUserId(userId);

			return activeGroups.stream()
				.map(groupMember -> {
					Group group = groupMember.getGroup();
					int authCount = calculateGroupAuthCount(userId, group.getGroupId(), monthYear);

					return PersonalRankingResponse.GroupRankingDetail.builder()
						.groupId(group.getGroupId())
						.groupName(group.getGroupName())
						.authCount(authCount)
						.groupType(group.getGroupType().name())
						.build();
				})
				.collect(Collectors.toList());
		} catch (Exception e) {
			log.warn("그룹별 상세 정보 조회 실패: 사용자 ID = {}", userId, e);
			return Collections.emptyList();
		}
	}

	private int calculateGroupAuthCount(Long userId, Long groupId, String monthYear) {
		try {
			LocalDate startDate = LocalDate.parse(monthYear + "-01");
			LocalDate endDate = startDate.plusMonths(1).minusDays(1);

			return (int) userActivityRepository
				.countByUserIdAndActivityTypeAndCreatedAtBetween(
					userId,
					ActivityType.GROUP_AUTH_COMPLETE,
					startDate.atStartOfDay(),
					endDate.atTime(23, 59, 59)
				);
		} catch (Exception e) {
			log.warn("그룹 인증 횟수 계산 실패: 사용자 ID = {}, 그룹 ID = {}", userId, groupId, e);
			return 0;
		}
	}

	private double getGroupWeightMultiplier(Group group) {
		if (group == null || group.getGroupType() == null) {
			return 1.0;
		}

		return switch (group.getGroupType()) {
			case REQUIRED -> // 의무 참여
				1.5;
			case FREE -> // 자유 참여
				1.2;
			default -> 1.0;
		};
	}

	private double calculateConsecutiveBonus(int consecutiveDays) {
		if (consecutiveDays <= 2 && consecutiveDays < 30) {
			return consecutiveDays * 0.5;
		} else if (consecutiveDays >= 30) {
			return 15;
		}
		return 0;
	}

	private int calculateGroupMembersTotalScore(Long groupId) {
		List<Ranking> memberRankings = rankingRepository.findAllUsersByGroupIdOrderByScore(groupId);
		return memberRankings.stream().mapToInt(ranking -> {
			int baseScore = ranking.getScore();

			int consecutiveDays = calculateConsecutiveDays(ranking.getUserId());
			double consecutiveBonus = calculateConsecutiveBonus(consecutiveDays);

			return baseScore + (int)consecutiveBonus;
		}).sum();
	}

	private int calculateSimpleParticipationBonus(Long groupId, String monthYear) {
		try {
			int memberCount = groupMemberRepository.countMembersByGroupId(groupId);
			int activeMembers = groupMemberRepository.countActiveByGroupId(groupId, monthYear);

			if (memberCount == 0) {
				log.debug("그룹 {} 멤버 수가 0명이므로 보너스 0점", groupId);
				return 0;
			}

			double participationRate = (double) activeMembers / memberCount;

			int participationBonus = 0;
			if (participationRate >= 0.8) {
				participationBonus = 15;
			} else if (participationRate >= 0.6) {
				participationBonus = 10;
			} else if (participationRate >= 0.4) {
				participationBonus = 5;
			}

			int memberCountBonus = 0;
			if (memberCount >= 10) {
				memberCountBonus = 10;
			} else if (memberCount >= 5) {
				memberCountBonus = 5;
			}

			int totalBonus = participationBonus + memberCountBonus;

			return totalBonus;

		} catch (Exception e) {
			log.warn("그룹 {} 참여도 보너스 계산 실패: {}", groupId, e.getMessage());
			return 0;
		}
	}

	@Getter
	private static class GroupScoreData {
		private final Long groupId;
		private final Group group;
		private final int finalScore;
		private final int membersTotalScore;
		private final int participationBonus;

		public GroupScoreData(Long groupId, Group group, int finalScore, int membersTotalScore, int participationBonus) {
			this.groupId = groupId;
			this.group = group;
			this.finalScore = finalScore;
			this.membersTotalScore = membersTotalScore;
			this.participationBonus = participationBonus;
		}
	}
}