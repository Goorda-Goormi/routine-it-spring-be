package com.goormi.routine.domain.calendar.dto;

import com.goormi.routine.domain.calendar.entity.UserCalendar;
import com.goormi.routine.domain.calendar.entity.UserCalendar.CalendarColor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 캘린더 응답 DTO들
 */
@Builder
public record CalendarResponse(
        Long id,
        Long userId,
        String subCalendarId,
        String calendarName,
        CalendarColor color,
        Integer reminderMinutes,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static CalendarResponse from(UserCalendar userCalendar) {
        return CalendarResponse.builder()
                .id(userCalendar.getId())
                .userId(userCalendar.getUser().getId())
                .subCalendarId(userCalendar.getSubCalendarId())
                .calendarName(userCalendar.getCalendarName())
                .color(userCalendar.getColor())
                .reminderMinutes(userCalendar.getReminderMinutes())
                .active(userCalendar.isActive())
                .createdAt(userCalendar.getCreatedAt())
                .updatedAt(userCalendar.getUpdatedAt())
                .build();
    }
}
