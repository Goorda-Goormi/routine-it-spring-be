package com.goormi.routine.domain.calendar.service;

import com.goormi.routine.domain.calendar.client.KakaoCalendarClient;
import com.goormi.routine.domain.calendar.exception.KakaoApiException;
import com.goormi.routine.domain.personal_routines.domain.PersonalRoutine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Optional;

import static com.goormi.routine.domain.calendar.dto.KakaoCalendarDto.*;

/**
 * 캘린더 서비스 개인 루틴용 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class CalendarPersonalServiceImpl implements CalendarPersonalService {

    private final KakaoCalendarClient kakaoCalendarClient;
    private final KakaoTokenService kakaoTokenService;
    private final CalendarService calendarService;

    /**
     * 개인 일정 생성
     */

    @Override
    @Transactional
    public String createPersonalSchedule(Long userId, PersonalRoutine personalRoutine) {
        log.info("개인 일정 생성 시작: userId={}, routineId={}", userId, personalRoutine.getRoutineId());

        try {
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);

            // 개인루틴 정보를 바탕으로 일정 생성
            CreateEventRequest request = buildEventRequest("primary", personalRoutine);
            CreateEventResponse response = kakaoCalendarClient.createEvent(accessToken, "primary", request);

            log.info("개인 일정 생성 완료: userId={}, eventId={}", userId, response.eventId());

            return response.eventId();

        } catch (Exception e) {
            log.error("개인 일정 생성 실패: userId={}, RoutineId={}", userId, personalRoutine.getRoutineId(), e);
            throw new KakaoApiException("개인 일정 생성에 실패했습니다", e, 500, "EVENT_CREATE_FAILED");
        }
    }

    /**
     * 개인 일정 수정
     */
    @Override
    @Transactional
    public String updatePersonalSchedule(Long userId, PersonalRoutine personalRoutine, String eventId) {
        log.info("개인 일정 수정 시작: userId={}, eventId={}", userId, eventId);

        // 입력 매개변수 유효성 검사
        if (eventId == null || eventId.trim().isEmpty()) {
            log.error("eventId가 null이거나 비어있습니다: '{}'", eventId);
            throw new IllegalArgumentException("eventId는 필수값입니다");
        }

        if (personalRoutine == null) {
            log.error("personalRoutine 객체가 null입니다");
            throw new IllegalArgumentException("personalRoutine 객체는 필수값입니다");
        }

        log.debug("입력 검증 완료 - eventId: '{}', RoutineName: '{}'", eventId, personalRoutine.getRoutineName());

        try {
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);
            log.debug("액세스 토큰 획득 완료");

            GetEventsResponse events = getPersonalEvents(userId);
            String actualEventId = eventId;
            String startAt = null;
            if (events != null && events.events() != null){
//                Arrays.stream(events.events()).forEach(e -> log.info("Event id: {}, title: {}", e.id(), e.title()));

                Optional<EventBrief> matchedEventOpt = Arrays.stream(events.events())
                        .filter(e -> e.id() != null && e.id().startsWith(eventId))
                        .findFirst();

                actualEventId = matchedEventOpt.map(EventBrief::id).orElse(null);
                startAt = matchedEventOpt.map(e -> e.time().startAt()).orElse(null);

            }
            String calendarId = "primary";
            UpdateEventRequest request = buildUpdateEventRequest(personalRoutine, actualEventId, calendarId, startAt);
            log.debug("일정 수정 요청 생성 완료: actualEventId={}, calendarId={}", actualEventId, calendarId);

            // 카카오 API 호출 전 최종 검증 로그
            log.info("카카오 API 호출 시작 - actualEventId: '{}', calendarId: '{}', userId: {}",
                    actualEventId, calendarId, userId);

            try {
                UpdateEventResponse updateEventResponse = kakaoCalendarClient.updateEvent(accessToken, actualEventId, request);
                log.info("개인 일정 수정 완료: userId={}, eventId={}", actualEventId, eventId);
                return updateEventResponse != null ? updateEventResponse.eventId() : actualEventId;
            } catch (RuntimeException kakaoApiException) {
                log.error("카카오 API 오류 발생: actualEventId={}, calendarId={}, error={}",
                        actualEventId, calendarId, kakaoApiException.getMessage());

                // 카카오 API에서 이벤트나 캘린더를 찾을 수 없는 경우
                if (kakaoApiException.getMessage().contains("Invalid calendar_id or event_id") ||
                        kakaoApiException.getMessage().contains("not found") ||
                        kakaoApiException.getMessage().contains("400")) {

                    log.error("이벤트를 찾을 수 없음: actualEventId={}, calendarId={}, 상세오류={}",
                            actualEventId, calendarId, kakaoApiException.getMessage());

                    // 일정 수정 실패 시 새 일정 생성하지 않고 오류 전파
                    throw new KakaoApiException(
                            "해당 이벤트를 찾을 수 없습니다. actualEventId: " + actualEventId + ", calendarId: " + calendarId,
                            kakaoApiException, 404, "EVENT_NOT_FOUND");
                } else {
                    throw kakaoApiException; // 다른 오류는 그대로 전파
                }
            }

        } catch (RuntimeException e) {
            // 우리가 던진 검증 오류인지 카카오 API 오류인지 구분
            if (e.getMessage().contains("권한이 없습니다") || e.getMessage().contains("연동되어 있지 않습니다")) {
                log.error("권한/연동 오류: {}", e.getMessage());
                throw e; // 그대로 다시 던짐
            } else {
                log.error("카카오 API 호출 중 오류 발생: userId={}, eventId={}, error={}",
                        userId, eventId, e.getMessage(), e);
                throw new KakaoApiException("카카오 캘린더 API 호출에 실패했습니다: " + e.getMessage(),
                        e, 500, "KAKAO_API_ERROR");
            }
        } catch (Exception e) {
            log.error("개인 일정 수정 실패: userId={}, eventId={}", userId, eventId, e);
            throw new KakaoApiException("개인 일정 수정에 실패했습니다", e, 500, "EVENT_UPDATE_FAILED");
        }
    }

    /**
     * 개인 일정 삭제
     */
    @Override
    @Transactional
    public void deletePersonalSchedule(String eventId, Long userId) {
        log.info("그룹 일정 삭제 시작: eventId={}, userId={}", eventId, userId);

        try {
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);

            GetEventsResponse events = getPersonalEvents(userId);
            String actualEventId = eventId;
            if (events != null && events.events() != null){
                actualEventId = Arrays.stream(events.events())
                        .map(EventBrief::id)
                        .filter(id -> id !=null && id.startsWith(eventId))
                        .findFirst()
                        .orElse(eventId); // null 대신 기본 eventId를 사용하도록 수정
            }

            // 삭제 요청 DTO 생성 (모든 반복 일정 삭제)
            DeleteEventRequest request = DeleteEventRequest.builder()
                    .eventId(actualEventId)
                    .recurUpdateType("ALL")
                    .build();

            kakaoCalendarClient.deleteEvent(accessToken, request);
            log.info("개인 일정 삭제 완료: actualEventId={}", actualEventId);

        } catch (Exception e) {
            log.error("개인 일정 삭제 실패: eventId={}", eventId, e);
            throw new KakaoApiException("개인 일정 삭제에 실패했습니다", e, 500, "EVENT_DELETE_FAILED");
        }
    }

    /**
     * 캘린더 내의 이벤트 조회
     */
    @Override
    public GetEventsResponse getPersonalEvents(Long userId) {
        try {
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);
            log.debug("액세스 토큰 획득 완료");

            // 카카오 API는 UTC 기준의 RFC3339 형식을 요구합니다.
            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

            // 현재 시각과 14일 후 시각을 UTC 기준으로 포맷팅합니다.
            String from = formatter.format(Instant.now());
            String to = formatter.format(Instant.now().plus(14, ChronoUnit.DAYS));

            GetEventsRequest request = GetEventsRequest.builder()
                    .calendarId("primary")
                    .from(from)
                    .to(to)
                    .build();

            GetEventsResponse response = kakaoCalendarClient.getEvents(accessToken, request);
            return response;
        }  catch (Exception e) {
            throw new KakaoApiException("카카오 일정 조회에 실패했습니다.", e, 500, "GET_EVENTS_FAILED");
        }
    }

    /**
     * 개인 정보를 바탕으로 일정 생성 요청 빌드
     */
    private CreateEventRequest buildEventRequest(String subCalendarId, PersonalRoutine personalRoutine) {
        log.debug("개인루틴 정보 확인:");
        log.debug("- routineName: {}", personalRoutine.getRoutineName());
        log.debug("- description: {}", personalRoutine.getDescription());
        log.debug("- alarmTime: {}", personalRoutine.getStartTime());
        log.debug("- authDays: {}", personalRoutine.getRepeatDays());

        String startTime = calendarService.formatAlarmTime(personalRoutine.getStartTime());
        String endTime = calendarService.formatAlarmTime(personalRoutine.getStartTime().plusMinutes(30)); // 30분 일정으로 설정
        String recurRule = calendarService.buildRecurRule(personalRoutine.getRepeatDays());

        log.debug("생성된 값들:");
        log.debug("- startTime: {}", startTime);
        log.debug("- endTime: {}", endTime);
        log.debug("- recurRule: {}", recurRule);

        // EventTime 객체 생성
        Time time = Time.builder()
                .startAt(startTime)
                .endAt(endTime)
                .build();

        log.debug("EventTime 생성: {}", time);

        log.debug("일정 생성 요청 빌드:");
        log.debug("- subCalendarId: {}", subCalendarId);
        log.debug("- title: {}", personalRoutine.getRoutineName());
        log.debug("- description: {}", personalRoutine.getDescription());
        log.debug("- time.start_at: {}", startTime);
        log.debug("- time.end_at: {}", endTime);
        log.debug("- startTime: {}", startTime);
        log.debug("- endTime: {}", endTime);
        log.debug("- recurRule: {}", recurRule);
        log.debug("- authDays: {}", personalRoutine.getRepeatDays());

        Integer[] reminders = new Integer[]{5,5};
        if (!personalRoutine.getIsAlarmOn()){
            reminders = null;
        }

        EventCreate eventCreate = EventCreate.builder()
                .title(personalRoutine.getRoutineName())
                .description(personalRoutine.getDescription())
                .time(time)  // EventTime 객체 설정
                .rrule(recurRule)
                .reminders(reminders)
                .build();

        log.debug("EventCreate 객체 생성:");
        log.debug("- title: {}", eventCreate.title());
        log.debug("- description: {}", eventCreate.description());
        log.debug("- time: {}", eventCreate.time());
        log.debug("- rrule: {}", eventCreate.rrule());

        CreateEventRequest createEventRequest = CreateEventRequest.builder()
                .calendarId(subCalendarId)
                .event(eventCreate)
                .build();

        log.debug("CreateEventRequest 생성 완료: event={}", createEventRequest.event());

        return createEventRequest;
    }

    /**
     * 개인 정보를 바탕으로 일정 수정 요청 빌드
     */
    private UpdateEventRequest buildUpdateEventRequest(PersonalRoutine personalRoutine, String eventId, String calendarId, String startDate) {
        log.debug("UpdateEventRequest 빌드 시작: groupName={}, eventId={}, calendarId={}",
                personalRoutine.getRoutineName(), eventId, calendarId);

        Time time = calendarService.calculateEventTime(startDate, personalRoutine.getStartTime());
        String recurRule = calendarService.buildRecurRule(personalRoutine.getRepeatDays());

        log.debug("시간 정보 생성:");
        log.debug("- startTime: {}", time.startAt());
        log.debug("- endTime: {}", time.endAt());
        log.debug("- recurRule: {}", recurRule);

        Integer[] reminders = new Integer[]{5,5};
        if (!personalRoutine.getIsAlarmOn()){
            reminders = null;
        }

        EventUpdate eventUpdate = EventUpdate.builder()
                .title(personalRoutine.getRoutineName())
                .description(personalRoutine.getDescription())
                .time(time)
                .rrule(recurRule)
                .reminders(reminders)
                .build();

        log.debug("EventUpdate 생성 완료:");
        log.debug("- title: {}", eventUpdate.title());
        log.debug("- description: {}", eventUpdate.description());
        log.debug("- rrule: {}", eventUpdate.rrule());
        log.debug("- time: startAt={}, endAt={}",
                eventUpdate.time() != null ? eventUpdate.time().startAt() : "null",
                eventUpdate.time() != null ? eventUpdate.time().endAt() : "null");

        UpdateEventRequest request = UpdateEventRequest.builder()
                .event(eventUpdate)
                .eventId(eventId)
                .calendarId(calendarId)
                .recurUpdateType("ALL")
                .build();

        log.debug("UpdateEventRequest 생성 완료: eventId={}, calendarId={}, recurUpdateType={}",
                request.eventId(), request.calendarId(), request.recurUpdateType());

        return request;
    }

}
