package com.goormi.routine.domain.calendar.exception;

/**
 * 캘린더를 찾을 수 없을 때 발생하는 예외
 */
public class CalendarNotFoundException extends RuntimeException {
    public CalendarNotFoundException(String message) {
        super(message);
    }
    
    public CalendarNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
