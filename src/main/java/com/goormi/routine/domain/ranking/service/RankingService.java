package com.goormi.routine.domain.ranking.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.goormi.routine.domain.ranking.dto.GlobalGroupRankingResponse;
import com.goormi.routine.domain.ranking.dto.GroupTop3RankingResponse;
import com.goormi.routine.domain.ranking.dto.PersonalRankingResponse;

public interface RankingService {

	Page<PersonalRankingResponse> getPersonalRankings(Pageable pageable, Long currentUserId);

	GlobalGroupRankingResponse getGlobalGroupRankings(
		String category, String groupType, Pageable pageable);

	GroupTop3RankingResponse getTop3RankingsByGroup(Long groupId);

	void updateRankingScore(Long userId, Long groupId, int score);

	void updateGroupScore(Long userId, Long groupId, int score, int consecutiveDays, String monthYear);

	void resetMonthlyRankings();

	long getTotalScoreByUser(Long userId);

	void initializeRanking(Long userId, Long groupId);
}
