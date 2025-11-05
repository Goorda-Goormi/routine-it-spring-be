package com.goormi.routine.domain.calendar.exception;

/**
 * 카카오 API 호출 실패 시 발생하는 예외
 */
public class KakaoApiException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;
    
    public KakaoApiException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public KakaoApiException(String message, Throwable cause, int statusCode, String errorCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
