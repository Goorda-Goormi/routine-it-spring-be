package com.goormi.routine.domain.ranking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.group.repository.GroupRepository;
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
	private final GroupRepository groupRepository;
	private final GroupMemberRepository groupMemberRepository;
	private final RankingRedisRepository rankingRedisRepository;
	private final UserActivityRepository userActivityRepository;

	@Override
	public Page<PersonalRankingResponse> getPersonalRankings(String monthYear, Pageable pageable, Long currentUserId) {
		Page<Object[]> rankingPage = rankingRepository.findPersonalRankingsByMonth(monthYear, pageable);

		List<PersonalRankingResponse> rankings = new ArrayList<>();
		int startRank = pageable.getPageNumber() * pageable.getPageSize() + 1;

		for (int i = 0; i < rankingPage.getContent().size(); i++) {
			Object[] row = rankingPage.getContent().get(i);
			Long userId = ((Number) row[0]).longValue();
			Integer totalScore = ((Number) row[1]).intValue();

			User user = userRepository.findById(userId).orElse(null);
			boolean isCurrentUser = currentUserId != null && currentUserId.equals(userId);


			PersonalRankingResponse response = PersonalRankingResponse.builder()
				.currentRank(startRank + i)
				.userId(userId)
				.nickname(user != null ? user.getNickname() : "탈퇴한 사용자")
				.totalScore(totalScore)
				.totalParticipants((int) rankingPage.getTotalElements())
				.monthYear(monthYear)
				.consecutiveDays(calculateConsecutiveDays(userId))
				.groupDetails(getGroupDetailsByUserId(userId, monthYear))
				.isCurrentUser(isCurrentUser) // 현재 사용자 여부 추가
				.updatedAt(LocalDateTime.now())
				.build();

			rankings.add(response);
		}

		return new PageImpl<>(rankings, pageable, rankingPage.getTotalElements());
	}

	@Override
	public Page<GlobalGroupRankingResponse.GroupRankingItem> getGlobalGroupRankings(
		String monthYear, String category, String groupType, Pageable pageable
	) {
		Page<Object[]> groupPage = rankingRepository.findGroupRankingsByMonthAndFilters(
			monthYear, category, groupType, pageable);

		List<GlobalGroupRankingResponse.GroupRankingItem> rankings = new ArrayList<>();
		int startRank = pageable.getPageNumber() * pageable.getPageSize() + 1;

		for (int i = 0; i < groupPage.getContent().size(); i++) {
			Object[] row = groupPage.getContent().get(i);
			Long groupId = ((Number) row[0]).longValue();

			GroupScoreData scoreData = calculateGroupScore(groupId, monthYear);

			if (scoreData.getGroup() != null) {
				Group group = scoreData.getGroup();

				int memberCount = groupMemberRepository.countMembersByGroupId(groupId);
				int activeMembers = groupMemberRepository.countActiveByGroupId(groupId, monthYear);
				int totalAuthCount = groupMemberRepository.countAuthByGroupId(groupId, monthYear);

				double participationRate = memberCount > 0 ? (double)activeMembers / memberCount : 0.0;
				double averageAuthPerMember = memberCount > 0 ? (double)totalAuthCount / memberCount : 0.0;

				GlobalGroupRankingResponse.GroupRankingItem item =
					GlobalGroupRankingResponse.GroupRankingItem.builder()
						.rank(startRank + i)
						.groupId(groupId)
						.groupName(group.getGroupName())
						.groupImageUrl(group.getGroupImageUrl())
						.category(group.getCategory())
						.groupType(group.getGroupType().name())
						.totalScore(scoreData.getFinalScore())
						.memberCount(memberCount)
						.activeMembers(activeMembers)
						.participationRate(Math.round(participationRate * 100.0) / 100.0)
						.totalAuthCount(totalAuthCount)
						.averageAuthPerMember(Math.round(averageAuthPerMember * 100.0) / 100.0)
						.build();

				rankings.add(item);
			}
		}

		return new PageImpl<>(rankings, pageable, groupPage.getTotalElements());
	}

	@Override
	public GroupTop3RankingResponse getTop3RankingsByGroup(Long groupId, String monthYear) {
		if (groupId == null) {
			throw new IllegalArgumentException("그룹 ID는 필수입니다.");
		}

		Group group = groupRepository.findById(groupId)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 그룹입니다."));

		List<Ranking> top3Rankings = rankingRepository.findTop3UsersByGroupId(groupId);

		if (top3Rankings.isEmpty()) {
			return GroupTop3RankingResponse.builder()
				.groupId(groupId)
				.top3Users(Collections.emptyList())
				.totalMembers(0)
				.monthYear(monthYear)
				.updatedAt(LocalDateTime.now())
				.build();
		}

		double groupWeightMultiplier = getGroupWeightMultiplier(group);

		List<GroupTop3RankingResponse.UserRankingItem> userRankingItems =
			IntStream.range(0, top3Rankings.size())
				.mapToObj(index -> {
					Ranking ranking = top3Rankings.get(index);
					User user = ranking.getUser();

					int authCount = calculateGroupAuthCount(ranking.getUserId(), groupId, monthYear);
					int consecutiveDays = calculateConsecutiveDays(ranking.getUserId());

					int finalScore = ranking.getScore();

					int baseScore = authCount * 10;
					double consecutiveBonus = calculateConsecutiveBonus(consecutiveDays);

					GroupTop3RankingResponse.ScoreBreakdown scoreBreakdown =
						GroupTop3RankingResponse.ScoreBreakdown.builder()
							.baseScore(baseScore)
							.weightMultiplier(groupWeightMultiplier)
							.weightedScore((int)(baseScore * groupWeightMultiplier))
							.consecutiveBonus(consecutiveBonus)
							.finalScore(finalScore)
							.build();

					return GroupTop3RankingResponse.UserRankingItem.builder()
						.rank(index + 1)
						.userId(ranking.getUserId())
						.nickname(user != null ? user.getNickname() : "탈퇴한 사용자")
						.profileImageUrl(user != null ? user.getProfileImageUrl() : null)
						.score(finalScore)
						.authCount(authCount)
						.consecutiveDays(consecutiveDays)
						.consecutiveBonus(consecutiveBonus)
						.scoreBreakdown(scoreBreakdown)
						.build();
				})
				.collect(Collectors.toList());

		return GroupTop3RankingResponse.builder()
			.groupId(groupId)
			.groupName(group.getGroupName())
			.groupType(group != null ? group.getGroupType().name() : null)
			.groupWeightMultiplier(groupWeightMultiplier)
			.monthYear(monthYear)
			.top3Users(userRankingItems)
			.totalMembers(groupMemberRepository.countMembersByGroupId(groupId))
			.updatedAt(LocalDateTime.now())
			.build();
	}

	@Override
	@Transactional
	public void updateRankingScore(Long userId, Long groupId, int authCount) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 필수입니다.");
		}
		if (groupId == null) {
			throw new IllegalArgumentException("그룹 ID는 필수입니다.");
		}
		if (authCount < 0) {
			throw new IllegalArgumentException("인증 횟수는 0 이상이어야 합니다.");
		}

		int baseScore = authCount * 10;
		int consecutiveDays = calculateConsecutiveDays(userId);
		double consecutiveBonus = calculateConsecutiveBonus(consecutiveDays);
		int finalScore = baseScore + (int)consecutiveBonus;

		updateGroupScore(userId, groupId, finalScore);
	}

	@Override
	@Transactional
	public void updateGroupScore(Long userId, Long groupId, int finalScore) {
		Optional<Ranking> existingRanking = rankingRepository.findByUserIdAndGroupId(userId, groupId);

		if (existingRanking.isPresent()) {
			Ranking ranking = existingRanking.get();
			ranking.setScore(ranking.getScore() + finalScore);
			ranking.setUpdatedAt(LocalDateTime.now());
			rankingRepository.save(ranking);
			log.info("그룹 점수 업데이트: 사용자 ID = {}, 그룹 ID = {}, 기본 점수 = {}, 총 점수 = {}",
				userId, groupId, finalScore, ranking.getScore());
		} else {
			initializeRanking(userId, groupId);
			updateGroupScore(userId, groupId, finalScore);
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
			.mapToInt(Ranking::getScore)
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

	private GroupScoreData calculateGroupScore(Long groupId, String monthYear) {
		List<Ranking> memberRankings = rankingRepository.findAllUsersByGroupIdAndMonthOrderByScore(groupId, monthYear);

		if (memberRankings.isEmpty()) {
			return new GroupScoreData(groupId, null, 0, 0, 0);
		}

		Group group = memberRankings.get(0).getGroup();
		int membersTotalScore = memberRankings.stream().mapToInt(Ranking::getScore).sum();
		int participationBonus = calculateSimpleParticipationBonus(groupId, monthYear);
		int finalScore = membersTotalScore + participationBonus;

		return new GroupScoreData(groupId, group, finalScore, membersTotalScore, participationBonus);
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
		return memberRankings.stream().mapToInt(Ranking::getScore).sum();
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