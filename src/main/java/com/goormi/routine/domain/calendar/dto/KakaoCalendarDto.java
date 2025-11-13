package com.goormi.routine.domain.calendar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * 카카오 캘린더 API 요청/응답을 위한 DTO들
 */
public class KakaoCalendarDto {

    /**
     * 서브캘린더 조회 요청
     */
    @Builder
    public record GetCalendarsRequest(
            String filter
    ){}
    public record GetCalendarsResponse(
            Calendar[] calendars
    ){}
    public record Calendar(
            String id, String name
    ){}

    /**
     * 서브캘린더 생성 요청 DTO
     */
    @Builder
    public record CreateSubCalendarRequest(
            String name,
            String color,
            Integer reminder,
            @JsonProperty("reminder_all_day") Integer reminderAllDay
    ) {}
    /**
     * 서브캘린더 생성 응답 DTO
     */
    public record CreateSubCalendarResponse(
            @JsonProperty("calendar_id") String subCalendarId
    ) {}

    /**
     * 서브캘린더 삭제 요청 DTO
     */
    public record DeleteSubCalendarRequest(
            @JsonProperty("calendar_id") String subCalendarId
    ) {}
    /**
     * 서브캘린더 삭제 응답 DTO
     */
    public record DeleteSubCalendarResponse(
            @JsonProperty("calendar_id") String subCalendarId
    ) {}


    /**
     * 일정 조회 요청
     * curl -v -G GET "https://kapi.kakao.com/v2/api/calendar/events" \
     *     -H "Authorization: Bearer ${ACCESS_TOKEN}" \
     *     -d "calendar_id=user_63759daa38e1f752188e0cc9" \
     *     -d "from=2022-10-26T00:00:00Z" \
     *     -d "to=2022-10-30T00:00:00Z" \
     *     -d "limit=2"
     *
     */
    @Builder
    public record GetEventsRequest(
            @JsonProperty("calendar_id") String calendarId,
            String from,
            String to

    ){}
    public record GetEventsResponse(
            EventBrief[] events,
            Boolean has_next
    ){}
    public record EventBrief(
            String id,
            String title,
            Time time,
            @JsonProperty("calendar_id") String calendarId
    ){}




    /**
     * 일정 생성 요청 DTO (API 호출용)
     */
    @Builder
    public record CreateEventRequest(
            @JsonProperty("calendar_id") String calendarId,
            EventCreate event
    ) {}
    @Builder
    public record EventCreate(
            String title,
            Time time,  // 일정 시간 객체
            String rrule,
            String description,
            Integer[] reminders
            ) {}
    @Builder
    public record Time(
            @JsonProperty("start_at") String startAt,
            @JsonProperty("end_at") String endAt
    ) {}


    /**
     * 일정 생성 응답 DTO
     */
    public record CreateEventResponse(
            @JsonProperty("event_id") String eventId
    ) {}


    /**
     * 일정 수정 요청 DTO
     */
    @Builder
    public record UpdateEventRequest(
            @JsonProperty("event_id") String eventId,
            @JsonProperty("calendar_id") String calendarId,
            @JsonProperty("recur_update_type") String recurUpdateType,
            EventUpdate event
    ) {
        // 기본값으로 THIS_AND_FOLLOWING 설정
        public UpdateEventRequest {
            if (recurUpdateType == null) {
                recurUpdateType = "ALL";
            }
        }
    }
    @Builder
    public record EventUpdate(
            String title,
            Time time,
            String rrule,
            String description,
            Integer[] reminders
    ) {}

    /**
     * 일정 수정 응답 DTO
     */
    public record UpdateEventResponse(
            @JsonProperty("event_id") String eventId
    ) {}

    /**
     * 일정 삭제 요청 DTO
     */
    @Builder
    public record DeleteEventRequest(
            @JsonProperty("event_id") String eventId,
            @JsonProperty("recur_update_type") String recurUpdateType
    ) {
        // 기본값으로 ALL 설정 (모든 반복 일정 삭제)
        public DeleteEventRequest {
            if (recurUpdateType == null) {
                recurUpdateType = "ALL";
            }
        }
    }

    /**
     * 일정 삭제 응답 DTO
     */
    public record DeleteEventResponse(
            @JsonProperty("event_id") String eventId
    ) {}

    /**
     * 카카오 API 에러 응답 DTO
     */
    public record KakaoErrorResponse(
            Integer code,
            String msg
    ) {}
}
