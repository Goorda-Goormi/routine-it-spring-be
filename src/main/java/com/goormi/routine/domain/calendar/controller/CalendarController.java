package com.goormi.routine.domain.calendar.controller;

import com.goormi.routine.common.response.ApiResponse;
import com.goormi.routine.domain.calendar.dto.CalendarResponse;
import com.goormi.routine.domain.calendar.dto.KakaoCalendarDto.GetCalendarsResponse;
import com.goormi.routine.domain.calendar.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 캘린더 컨트롤러
 * - Bean Validation을 통한 서버 측 검증
 * - 예외 처리는 @ControllerAdvice에서 공통 처리
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "캘린더", description = "카카오 캘린더 연동 관련 API (관리 및 테스트용)")
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * 캘린더 연동 (서브캘린더 생성)
     */
    @Operation(summary = "캘린더 연동", description = "사용자의 카카오 캘린더에 서브캘린더를 생성하여 연동합니다")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "캘린더 연동 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 연동된 캘린더")
    })
    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<CalendarResponse>> connectCalendar(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("=== 캘린더 연동 요청 받음 ===");
        log.info("요청 userId: {}", userId);
        
//        CalendarResponse response = calendarService.createUserCalendar(userId);
        
        log.info("캘린더 연동 요청 처리 완료: userId={}", userId);
//        return ResponseEntity.ok(ApiResponse.success());
        return ResponseEntity.notFound().build();
    }

    /**
     * 캘린더 연동 해제
     */
    @Operation(summary = "캘린더 연동 해제", description = "사용자의 카카오 캘린더 연동을 해제하고 서브캘린더를 삭제합니다")
    @DeleteMapping("/disconnect")
    public ResponseEntity<ApiResponse<Void>> disconnectCalendar(
            @AuthenticationPrincipal Long userId
    ) {
        log.info("=== 캘린더 연동 해제 요청 받음 ===");
        log.info("요청 userId: {}", userId);
        
        calendarService.deleteUserCalendar(userId);
        
        log.info("캘린더 연동 해제 요청 처리 완료: userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 내 캘린더 정보 조회
     */
    @Operation(summary = "내 캘린더 조회", description = "로그인된 사용자의 캘린더 정보를 조회합니다")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CalendarResponse>> getMyCalendar(
            @AuthenticationPrincipal Long userId
    ) {
        log.debug("캘린더 조회 요청: userId={}", userId);
        
        CalendarResponse response = calendarService.getUserCalendar(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 캘린더 연동 상태 확인
     */
    @Operation(summary = "캘린더 연동 상태 확인", description = "사용자의 캘린더 연동 상태를 확인합니다")
    @GetMapping("/connection-status")
    public ResponseEntity<ApiResponse<Boolean>> getConnectionStatus(
            @AuthenticationPrincipal Long userId
    ) {
        boolean connected = calendarService.isCalendarConnected(userId);
        return ResponseEntity.ok(ApiResponse.success(connected));
    }

    // === 테스트용 엔드포인트들 ===
    
    /**
     * 테스트용 캘린더 연동 (userId 직접 전달)
     */
    @Operation(summary = "[테스트용] 캘린더 연동", description = "테스트용: userId를 직접 전달하여 캘린더를 연동합니다")
    @PostMapping("/test/connect/{userId}")
    public ResponseEntity<ApiResponse<CalendarResponse>> testConnectCalendar(
            @PathVariable Long userId
    ) {
        log.info("=== [테스트용] 캘린더 연동 요청 받음 ===");
        log.info("요청 userId: {}", userId);
        
//        CalendarResponse response = calendarService.createUserCalendar(userId);
        
        log.info("[테스트용] 캘린더 연동 요청 처리 완료: userId={}", userId);
//        return ResponseEntity.ok(ApiResponse.success(response));
        return ResponseEntity.notFound().build();
    }

    /**
     * 테스트용 캘린더 연동 해제 (userId 직접 전달)
     */
    @Operation(summary = "[테스트용] 캘린더 연동 해제", description = "테스트용: userId를 직접 전달하여 캘린더 연동을 해제합니다")
    @DeleteMapping("/test/disconnect/{userId}")
    public ResponseEntity<ApiResponse<Void>> testDisconnectCalendar(
            @PathVariable Long userId
    ) {
        log.info("=== [테스트용] 캘린더 연동 해제 요청 받음 ===");
        log.info("요청 userId: {}", userId);
        
        calendarService.deleteUserCalendar(userId);
        
        log.info("[테스트용] 캘린더 연동 해제 요청 처리 완료: userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 테스트용 캘린더 연동 상태 확인 (userId 직접 전달)
     */
    @Operation(summary = "[테스트용] 캘린더 연동 상태 확인", description = "테스트용: userId를 직접 전달하여 캘린더 연동 상태를 확인합니다")
    @GetMapping("/test/connection-status/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> testGetConnectionStatus(
            @PathVariable Long userId
    ) {
        boolean connected = calendarService.isCalendarConnected(userId);
        return ResponseEntity.ok(ApiResponse.success(connected));
    }

    /**
     * 테스트용 카카오 캘린더 목록 조회 (userId 직접 전달)
     */
    @Operation(summary = "[테스트용] 카카오 캘린더 목록 조회", description = "테스트용: userId를 직접 전달하여 카카오 캘린더 목록을 조회합니다")
    @GetMapping("/test/kakao-calendars/{userId}")
    public ResponseEntity<ApiResponse<GetCalendarsResponse>> testGetKakaoCalendars(
            @PathVariable Long userId
    ) {
        log.info("=== [테스트용] 카카오 캘린더 목록 조회 요청 받음 ===");
        log.info("요청 userId: {}", userId);
        
        GetCalendarsResponse response = calendarService.getKakaoCalendars(userId);
        
        log.info("[테스트용] 카카오 캘린더 목록 조회 완료: userId={}, 캘린더 수={}", 
                userId, response.calendars().length);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
