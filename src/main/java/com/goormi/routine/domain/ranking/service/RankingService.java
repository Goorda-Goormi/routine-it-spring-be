package com.goormi.routine.domain.ranking.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.goormi.routine.domain.ranking.dto.GlobalGroupRankingResponse;
import com.goormi.routine.domain.ranking.dto.GroupTop3RankingResponse;
import com.goormi.routine.domain.ranking.dto.PersonalRankingResponse;

public interface RankingService {

	Page<PersonalRankingResponse> getPersonalRankings(String monthYear, Pageable pageable, Long currentUserId);

	Page<GlobalGroupRankingResponse.GroupRankingItem> getGlobalGroupRankings(
		String monthYear, String category, String groupType, Pageable pageable);

	GroupTop3RankingResponse getTop3RankingsByGroup(Long groupId, String monthYear);

	void updateRankingScore(Long userId, Long groupId, int score);

	void updateGroupScore(Long userId, Long groupId, int score);

	void resetMonthlyRankings();

	long getTotalScoreByUser(Long userId);

	void initializeRanking(Long userId, Long groupId);
}
