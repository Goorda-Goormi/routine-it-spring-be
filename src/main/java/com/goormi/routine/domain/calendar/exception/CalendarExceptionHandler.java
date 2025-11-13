package com.goormi.routine.domain.calendar.exception;

import com.goormi.routine.common.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 캘린더 도메인 전용 예외 처리기
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.goormi.routine.domain.calendar")
public class CalendarExceptionHandler {

    /**
     * 캘린더 이미 연동된 예외 처리
     */
    @ExceptionHandler(CalendarAlreadyConnectedException.class)
    public ResponseEntity<ErrorResponse> handleCalendarAlreadyConnectedException(
            CalendarAlreadyConnectedException ex) {
        
        log.error("Calendar already connected error: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * 캘린더 찾을 수 없음 예외 처리
     */
    @ExceptionHandler(CalendarNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCalendarNotFoundException(
            CalendarNotFoundException ex) {
        
        log.error("Calendar not found error: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(ex.getMessage()));
    }

    /**
     * 카카오 API 예외 처리
     */
    @ExceptionHandler(KakaoApiException.class)
    public ResponseEntity<ErrorResponse> handleKakaoApiException(
            KakaoApiException ex) {
        
        log.error("Kakao API error: {} (statusCode: {}, errorCode: {})", 
                ex.getMessage(), ex.getStatusCode(), ex.getErrorCode());
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(ErrorResponse.of("외부 API 호출에 실패했습니다: " + ex.getMessage()));
    }
}
