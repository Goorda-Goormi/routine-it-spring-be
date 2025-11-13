package com.goormi.routine.domain.calendar.exception;

/**
 * 캘린더가 이미 연동되어 있을 때 발생하는 예외
 * 복구 불가능한 시스템 예외는 언체크 예외로 처리
 */
public class CalendarAlreadyConnectedException extends RuntimeException {
    public CalendarAlreadyConnectedException(String message) {
        super(message);
    }
    
    public CalendarAlreadyConnectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
