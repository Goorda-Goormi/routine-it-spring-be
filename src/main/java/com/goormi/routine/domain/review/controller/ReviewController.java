package com.goormi.routine.domain.review.controller;

import com.goormi.routine.domain.auth.annotation.CurrentUser;
import com.goormi.routine.domain.review.service.ReviewService;
import com.goormi.routine.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "월간 회고", description = "월간 회고 시스템 API")
public class ReviewController {

	private final ReviewService reviewService;

	@Operation(
		summary = "월간 회고 메시지 전송",
		description = "매월 1일 사용자에게 월간 회고 메시지를 전달합니다. "
	)
	@PostMapping("/reviews/monthly")
	public ApiResponse<String> sendMonthlyReviewMessages(
		@CurrentUser Long userId,
		@RequestParam(required = false) String monthYear) {
		if (monthYear == null || monthYear.trim().isEmpty()) {
			monthYear = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"));
		}

			reviewService.sendUserReviewMessage(userId, monthYear);
			return ApiResponse.success("회고 메시지가 전송되었습니다.");
	}
}