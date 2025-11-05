package com.goormi.routine.domain.calendar.service;

public interface CalendarSyncService {
    void syncUserCalendar(Long userId, String accessToken);
}
