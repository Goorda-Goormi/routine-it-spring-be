package com.goormi.routine.domain.calendar.service;

import com.goormi.routine.domain.calendar.dto.CalendarResponse;
import com.goormi.routine.domain.calendar.dto.KakaoCalendarDto;
import com.goormi.routine.domain.calendar.dto.KakaoCalendarDto.GetCalendarsResponse;
import com.goormi.routine.domain.group.entity.Group;

import java.time.LocalTime;

public interface CalendarService {

    /**
     * 사용자 캘린더 생성 (회원가입 시 호출)
     */
    CalendarResponse createUserCalendar(Long userId, String accessToken);

    /**
     * 사용자 캘린더 삭제 (회원탈퇴 시 호출)
     */
    void deleteUserCalendar(Long userId);

    /**
     * 그룹 일정 생성 (그룹 가입 시 호출)
     */
    String createGroupSchedule(Long userId, Group group);

    /**
     * 그룹 일정 수정 (그룹 정보 변경 시 호출)
     */
    void updateGroupSchedule(Long userId, Group group, String eventId);

    /**
     * 그룹 일정 삭제 (그룹 탈퇴/삭제 시 호출)
     */
    void deleteGroupSchedule(String eventId, Long userId);

    /**
     * 사용자 캘린더 조회
     */
    CalendarResponse getUserCalendar(Long userId);

    /**
     * 캘린더 연동 상태 확인
     */
    boolean isCalendarConnected(Long userId);
    
    /**
     * 사용자의 카카오 캘린더 목록 조회 (테스트용)
     */
    GetCalendarsResponse getKakaoCalendars(Long userId);

    KakaoCalendarDto.Time calculateEventTime(String startDate, LocalTime alarmTime);

    String formatAlarmTime(LocalTime time);

    String buildRecurRule(String authDays);
}
