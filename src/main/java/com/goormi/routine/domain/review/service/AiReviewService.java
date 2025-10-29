package com.goormi.routine.domain.review.service;

import com.goormi.routine.domain.review.dto.MonthlyReviewResponse;

public interface AiReviewService {
	String generateAiMessage(MonthlyReviewResponse review) throws Exception;
}
