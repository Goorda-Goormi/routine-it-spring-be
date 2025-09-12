package com.goormi.routine.domain.review.service;

import com.goormi.routine.domain.review.dto.MonthlyReviewResponse;
import com.goormi.routine.domain.review.dto.UserReviewHistoryResponse;

public interface ReviewService {
	void sendMonthlyReviewMessages(String monthYear);
	void sendUserReviewMessage(Long userId, String monthYear);
	void retryFailedMessages(String monthYear);
	int getFailedMessageCount(String monthYear);

	MonthlyReviewResponse getMonthlyReview(Long userId, String monthYear);
}
