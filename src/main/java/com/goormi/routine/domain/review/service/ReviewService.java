package com.goormi.routine.domain.review.service;

import java.util.Map;

import com.goormi.routine.domain.review.dto.MonthlyReviewResponse;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.userActivity.entity.ActivityType;

public interface ReviewService {
	void sendMonthlyReviewMessages(String monthYear);
	void sendUserReviewMessage(Long userId, String monthYear);
	void sendReviewMessageBatch(Long userId, String monthYear, Map<Long, User> userMap,
		Map<Long, Map<ActivityType, Integer>> allActivityCounts,
		Map<Long, Long> allScores,
		Map<Long, Integer> allActiveGroupCounts,
		Map<Long, MonthlyReviewResponse> allPreviousReviews);
	void retryFailedMessages(String monthYear);
	int getFailedMessageCount(String monthYear);

	MonthlyReviewResponse getMonthlyReview(Long userId, String monthYear);
}
