package com.goormi.routine.domain.calendar.service;

import com.goormi.routine.domain.calendar.client.KakaoCalendarClient;
import com.goormi.routine.domain.calendar.dto.CalendarResponse;
import com.goormi.routine.domain.calendar.dto.KakaoCalendarDto.*;
import com.goormi.routine.domain.calendar.entity.UserCalendar;
import com.goormi.routine.domain.calendar.exception.CalendarNotFoundException;
import com.goormi.routine.domain.calendar.exception.KakaoApiException;
import com.goormi.routine.domain.calendar.repository.CalendarRepository;
import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.entity.GroupMemberStatus;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 캘린더 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class CalendarServiceImpl implements CalendarService {

    private final CalendarRepository calendarRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final KakaoCalendarClient kakaoCalendarClient;
    private final KakaoTokenService kakaoTokenService;

    /**
     * 사용자 캘린더 생성
     */
    @Override
    @Transactional
    public CalendarResponse createUserCalendar(Long userId, String accessToken) {
        log.info("사용자 캘린더 생성/확인 시작: userId={}", userId);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        UserCalendar userCalendar = calendarRepository.findByUserId(userId).orElse(null);

        // 1. DB에 캘린더 정보가 이미 있는 경우
        if (userCalendar != null) {
            log.debug("DB에서 사용자 캘린더 발견: userCalendarId={}", userCalendar.getId());
            try {
                // 카카오 API를 통해 실제 캘린더 목록 조회 (전달받은 토큰 사용)
                GetCalendarsResponse kakaoCalendars = kakaoCalendarClient.getCalendars(accessToken);
                boolean kakaoCalendarExists = Arrays.stream(kakaoCalendars.calendars())
                        .anyMatch(c -> c.id().equals(userCalendar.getSubCalendarId()));

                // 카카오 서버에도 캘린더가 존재하는 경우
                if (kakaoCalendarExists) {
                    log.info("카카오 서버에서 기존 캘린더 확인됨. 연동 상태를 확인하고 동기화합니다: subCalendarId={}", userCalendar.getSubCalendarId());
                    // DB 상태를 최신화
                    if (!userCalendar.isActive()) {
                        userCalendar.activate();
                    }
                    if (!user.getCalendarConnected()) {
                        user.connectCalendar();
                    }
                    log.info("기존 캘린더가 이미 연동되어 있습니다. 생성을 건너뜁니다: userId={}", userId);
                    return CalendarResponse.from(userCalendar);
                }
                // 카카오 서버에 캘린더가 없는 경우 (DB 정보가 오래된 경우)
                else {
                    log.warn("DB에는 캘린더 정보가 있으나 카카오 서버에 없습니다. DB 정보를 삭제하고 새로 생성합니다: subCalendarId={}", userCalendar.getSubCalendarId());
                    calendarRepository.delete(userCalendar);
                    calendarRepository.flush();
                }
            } catch (Exception e) {
                log.error("카카오 캘린더 확인 중 오류 발생. 캘린더를 새로 생성합니다.", e);
                // 기존 캘린더 정보 삭제 후 재생성 시도
                calendarRepository.delete(userCalendar);
                calendarRepository.flush();
            }
        }

        // 2. 새로 캘린더를 생성해야 하는 경우 (DB에 없거나, 카카오에 없어서 삭제된 경우)
        log.info("새로운 카카오 서브캘린더 생성을 시작합니다: userId={}", userId);
        try {
            // 카카오 서브캘린더 생성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd_HHmmss");
            String name = "routine-it for group_" + LocalDateTime.now().format(formatter);
            CreateSubCalendarRequest request = CreateSubCalendarRequest.builder()
                    .name(name)
                    .color("LIME")
                    .reminder(10)
                    .build();

            CreateSubCalendarResponse subCalendar = kakaoCalendarClient.createSubCalendar(accessToken, request);

            // UserCalendar 엔티티 생성 및 저장
            UserCalendar newUserCalendar = UserCalendar.createUserCalendar(user, subCalendar.subCalendarId());
            UserCalendar savedCalendar = calendarRepository.save(newUserCalendar);
            log.debug("저장된 UserCalendar 정보: id={}, userId={}, subCalendarId={}, active={}",
                    savedCalendar.getId(), savedCalendar.getUser().getId(),
                    savedCalendar.getSubCalendarId(), savedCalendar.isActive());

            // User 엔티티의 캘린더 연동 상태 업데이트
            user.connectCalendar();

            log.info("사용자 캘린더 생성 완료: userId={}, subCalendarId={}", userId, subCalendar.subCalendarId());

            return CalendarResponse.from(savedCalendar);

        } catch (Exception e) {
            log.error("캘린더 생성 실패: userId={}", userId, e);
            throw new KakaoApiException("캘린더 생성에 실패했습니다", e, 500, "CALENDAR_CREATE_FAILED");
        }
    }

    /**
     * 사용자 캘린더 삭제
     */
    @Override
    @Transactional
    public void deleteUserCalendar(Long userId) {
        log.info("사용자 캘린더 삭제 시작: userId={}", userId);
        
        UserCalendar userCalendar = calendarRepository.findByUserId(userId)
                .orElse(null);
        
        if (userCalendar == null) {
            log.warn("삭제할 캘린더가 없습니다: userId={}", userId);
            return;
        }
        
        try {
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);
            // 카카오 서브캘린더 삭제
            kakaoCalendarClient.deleteSubCalendar(accessToken, userCalendar.getSubCalendarId());
            
            // UserCalendar 엔티티 삭제
            calendarRepository.delete(userCalendar);
            
            // User 엔티티의 캘린더 연동 상태 업데이트
            User user = userCalendar.getUser();
            user.disconnectCalendar();
            
            log.info("사용자 캘린더 삭제 완료: userId={}, subCalendarId={}", userId, userCalendar.getSubCalendarId());
            
        } catch (Exception e) {
            log.error("캘린더 삭제 실패: userId={}", userId, e);
            // 카카오 API 실패해도 로컬 데이터는 정리
            calendarRepository.delete(userCalendar);
            userCalendar.getUser().disconnectCalendar();
        }
    }

    /**
     * 그룹 일정 생성
     */
    @Override
    @Transactional
    public String createGroupSchedule(Long userId, Group group) {
        log.info("그룹 일정 생성 시작: userId={}, groupId={}", userId, group.getGroupId());
        
        UserCalendar userCalendar = calendarRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new CalendarNotFoundException("활성화된 캘린더를 찾을 수 없습니다: " + userId));
        
        try {
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);
            
            // 그룹 정보를 바탕으로 일정 생성
            CreateEventRequest request = buildEventRequest(userCalendar.getSubCalendarId(), group);
            CreateEventResponse response = kakaoCalendarClient.createEvent(accessToken, userCalendar.getSubCalendarId(), request);
            
            log.info("그룹 일정 생성 완료: userId={}, eventId={}", userId, response.eventId());
            
            return response.eventId();
            
        } catch (Exception e) {
            log.error("그룹 일정 생성 실패: userId={}, groupId={}", userId, group.getGroupId(), e);
            throw new KakaoApiException("그룹 일정 생성에 실패했습니다", e, 500, "EVENT_CREATE_FAILED");
        }
    }

    /**
     * 그룹 일정 수정
     */
    @Override
    @Transactional
    public void updateGroupSchedule(Long userId, Group group, String eventId) {
        log.info("그룹 일정 수정 시작: userId={}, eventId={}", userId, eventId);
        
        // 입력 매개변수 유효성 검사
        if (eventId == null || eventId.trim().isEmpty()) {
            log.error("eventId가 null이거나 비어있습니다: '{}'", eventId);
            throw new IllegalArgumentException("eventId는 필수값입니다");
        }
        
        if (group == null) {
            log.error("group 객체가 null입니다");
            throw new IllegalArgumentException("group 객체는 필수값입니다");
        }
        
        log.debug("입력 검증 완료 - eventId: '{}', groupName: '{}'", eventId, group.getGroupName());
        
        try {
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);
            log.debug("액세스 토큰 획득 완료");
            
            // 사용자의 캘린더 ID 조회
            UserCalendar userCalendar = calendarRepository.findByUserIdAndActiveTrue(userId)
                    .orElseThrow(() -> new CalendarNotFoundException("캘린더가 연동되어 있지 않습니다: " + userId));
            String calendarId = userCalendar.getSubCalendarId();
            
            // calendarId 유효성 검사
            if (calendarId == null || calendarId.trim().isEmpty()) {
                log.error("calendarId가 null이거나 비어있습니다: '{}'", calendarId);
                throw new RuntimeException("유효하지 않은 calendar ID입니다: " + calendarId);
            }
            
            log.debug("캘린더 ID 조회 완료 - calendarId: '{}', userCalendarId: {}", 
                    calendarId, userCalendar.getId());
            
            // calendarId 형식 검증 
            if (!calendarId.startsWith("user_")) {
                log.warn("calendarId가 예상된 형식(user_로 시작)이 아닙니다: '{}'", calendarId);
            }
            
            // 해당 사용자가 실제로 이 eventId를 가진 그룹 멤버인지 확인
            // (권한 검증을 위해 해당 eventId가 사용자의 GroupMember에 저장되어 있는지 확인)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
            List<GroupMember> userGroupMembers = groupMemberRepository
                    .findAllByUserAndStatus(user, GroupMemberStatus.JOINED);
            
            boolean hasEventPermission = userGroupMembers.stream()
                    .anyMatch(member ->
                            member.getCalendarEventId() != null && eventId.startsWith(member.getCalendarEventId()));
            
            if (!hasEventPermission) {
                log.error("사용자 {}가 eventId {}에 대한 권한이 없습니다. 해당 이벤트가 사용자의 그룹 멤버 목록에 없습니다.", userId, eventId);
                throw new RuntimeException("해당 이벤트에 대한 수정 권한이 없습니다. eventId: " + eventId);
            }
            log.debug("이벤트 권한 확인 완료: userId={}, eventId={}", userId, eventId);

            GetEventsResponse events = getEvents(userId);
            String actualEventId = eventId;
            String startAt = null;
            if (events != null && events.events() != null){
                Optional<EventBrief> matchedEventOpt = Arrays.stream(events.events())
                        .filter(e -> e.id() != null && e.id().startsWith(eventId))
                        .findFirst();

                actualEventId = matchedEventOpt.map(EventBrief::id).orElse(eventId);
                startAt = matchedEventOpt.map(e -> e.time().startAt()).orElse(null);

            }

            GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, user).orElse(null);

            UpdateEventRequest request = buildUpdateEventRequest(group, actualEventId, calendarId, startAt, groupMember);
            log.debug("일정 수정 요청 생성 완료: actualEventId={}, calendarId={}", actualEventId, calendarId);
            
            // 카카오 API 호출 전 최종 검증 로그
            log.info("카카오 API 호출 시작 - actualEventId: '{}', calendarId: '{}', userId: {}",
                    actualEventId, calendarId, userId);
            
            try {
                kakaoCalendarClient.updateEvent(accessToken, actualEventId, request);
                log.info("그룹 일정 수정 완료: userId={}, eventId={}", actualEventId, eventId);
                
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
            log.error("그룹 일정 수정 실패: userId={}, eventId={}", userId, eventId, e);
            throw new KakaoApiException("그룹 일정 수정에 실패했습니다", e, 500, "EVENT_UPDATE_FAILED");
        }
    }

    /**
     * 그룹 일정 삭제
     */
    @Override
    @Transactional
    public void deleteGroupSchedule(String eventId, Long userId) {
        log.info("그룹 일정 삭제 시작: eventId={}, userId={}", eventId, userId);
        
        try {
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);

            GetEventsResponse events = getEvents(userId);
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
            log.info("그룹 일정 삭제 완료: actualEventId={}", actualEventId);
            
        } catch (Exception e) {
            log.error("그룹 일정 삭제 실패: actualEventId={}", eventId, e);
            throw new KakaoApiException("그룹 일정 삭제에 실패했습니다", e, 500, "EVENT_DELETE_FAILED");
        }
    }
    /**
     * 사용자의 카카오 캘린더 목록 조회
     */
    @Override
    public GetCalendarsResponse getKakaoCalendars(Long userId) {
        log.info("카카오 캘린더 목록 조회 시작: userId={}", userId);

        try {
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);
            log.debug("액세스 토큰 획득 완료");

            // 카카오 API 호출
            GetCalendarsResponse response = kakaoCalendarClient.getCalendars(accessToken);

            log.info("카카오 캘린더 목록 조회 완료: userId={}, 캘린더 수={}",
                    userId, response.calendars() != null ? response.calendars().length : 0);

            if (response.calendars() != null) {
                for (Calendar calendar : response.calendars()) {
                    log.debug("캘린더 정보: id={}, name={}", calendar.id(), calendar.name());
                }
            }

            return response;

        } catch (Exception e) {
            log.error("카카오 캘린더 목록 조회 실패: userId={}", userId, e);
            throw new KakaoApiException("카카오 캘린더 목록 조회에 실패했습니다", e, 500, "CALENDAR_LIST_FAILED");
        }
    }

    /**
     * 서브 캘린더 내의 이벤트 조회
     */
    public GetEventsResponse getEvents(Long userId) {
        try {
            String accessToken = kakaoTokenService.getKakaoAccessTokenByUserId(userId);
            log.debug("액세스 토큰 획득 완료");
            UserCalendar userCalendar = calendarRepository.findByUserId(userId)
                    .orElseThrow(() -> new CalendarNotFoundException("캘린더를 찾을 수 없습니다: " + userId));

            // 카카오 API는 UTC 기준의 RFC3339 형식을 요구합니다.
            DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

            // 현재 시각과 10일 후 시각을 UTC 기준으로 포맷팅합니다.
            String from = formatter.format(Instant.now());
            String to = formatter.format(Instant.now().plus(14, ChronoUnit.DAYS));

            GetEventsRequest request = GetEventsRequest.builder()
                    .calendarId(userCalendar.getSubCalendarId())
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
     * 사용자 캘린더(엔티티) 조회
     */
    @Override
    public CalendarResponse getUserCalendar(Long userId) {
        UserCalendar userCalendar = calendarRepository.findByUserId(userId)
                .orElseThrow(() -> new CalendarNotFoundException("캘린더를 찾을 수 없습니다: " + userId));
        
        return CalendarResponse.from(userCalendar);
    }

    /**
     * 캘린더 연동 상태 확인
     */
    @Override
    public boolean isCalendarConnected(Long userId) {
        // DB에서 활성화된 사용자 캘린더 정보 조회
        UserCalendar userCalendar = calendarRepository.findByUserIdAndActiveTrue(userId).orElse(null);
        if (userCalendar == null) {
            log.info("캘린더가 연동되어 있지 않습니다. (DB에 정보 없음): userId={}", userId);
            return false; // DB에 없으면 당연히 연동 안된 상태
        }

        try {
            // 카카오 API를 호출하여 실제 캘린더 목록 조회
            String token = kakaoTokenService.getKakaoAccessTokenByUserId(userId);
            GetCalendarsResponse response = kakaoCalendarClient.getCalendars(token);

            // DB의 subCalendarId와 실제 카카오 캘린더 목록을 비교
            boolean isCalendarMatched = Arrays.stream(response.calendars())
                    .anyMatch(calendar ->
                            Objects.equals(calendar.id(), userCalendar.getSubCalendarId()));

            if (!isCalendarMatched) {
                log.warn("DB에는 캘린더 정보가 있으나, 실제 카카오 서버에는 해당 서브캘린더가 없습니다. subCalendarId={}", userCalendar.getSubCalendarId());
            }

            log.debug("캘린더 연동 상태 확인 완료: userId={}, isConnected={}", userId, isCalendarMatched);
            return isCalendarMatched;

        } catch (Exception e) {
            log.error("카카오 캘린더 연동 상태 확인 중 오류 발생: userId={}", userId, e);
            // API 호출 실패 등 예외 발생 시, 안전하게 '연동되지 않음'으로 처리
            return false;
        }
    }

    /**
     * 그룹 정보를 바탕으로 일정 생성 요청 빌드
     */
    private CreateEventRequest buildEventRequest(String subCalendarId, Group group) {
        log.debug("그룹 정보 확인:");
        log.debug("- groupName: {}", group.getGroupName());
        log.debug("- description: {}", group.getDescription());
        log.debug("- alarmTime: {}", group.getAlarmTime());
        log.debug("- authDays: {}", group.getAuthDays());
        
        String startTime = formatAlarmTime(group.getAlarmTime());
        String endTime = formatAlarmTime(group.getAlarmTime().plusMinutes(30)); // 30분 일정으로 설정
        String recurRule = buildRecurRule(group.getAuthDays());
        
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
        log.debug("- title: {}", group.getGroupName());
        log.debug("- description: {}", group.getDescription());
        log.debug("- time.start_at: {}", startTime);
        log.debug("- time.end_at: {}", endTime);
        log.debug("- startTime: {}", startTime);
        log.debug("- endTime: {}", endTime);
        log.debug("- recurRule: {}", recurRule);
        log.debug("- authDays: {}", group.getAuthDays());

        Integer[] reminders = new Integer[]{5,5};
        if (!group.getIsAlarm()){
            reminders = null;
        }

        EventCreate eventCreate = EventCreate.builder()
                .title(group.getGroupName())
                .description(group.getDescription())
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
     * 그룹 정보를 바탕으로 일정 수정 요청 빌드
     */
    private UpdateEventRequest buildUpdateEventRequest(Group group, String eventId, String calendarId, String startDate, GroupMember groupMember) {
        log.debug("UpdateEventRequest 빌드 시작: groupName={}, eventId={}, calendarId={}",
                group.getGroupName(), eventId, calendarId);

        Time time = calculateEventTime(startDate, group.getAlarmTime());
        String recurRule = buildRecurRule(group.getAuthDays());

        log.debug("시간 정보 생성:");
        log.debug("- startTime: {}", time.startAt());
        log.debug("- endTime: {}", time.endAt());
        log.debug("- recurRule: {}", recurRule);

        Integer[] reminders = new Integer[]{5,5};
        if (groupMember != null && !groupMember.getIsAlarm()) {
            reminders = null;
        }

        EventUpdate eventUpdate = EventUpdate.builder()
                .title(group.getGroupName())
                .description(group.getDescription())
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
                .recurUpdateType("THIS_AND_FOLLOWING")
                .build();

        log.debug("UpdateEventRequest 생성 완료: eventId={}, calendarId={}, recurUpdateType={}",
                request.eventId(), request.calendarId(), request.recurUpdateType());

        return request;
    }

    @Override
    public Time calculateEventTime(String startDate, LocalTime alarmTime) {
        String startTime;
        String endTime;

        if (startDate != null) {
            // 시간대 명시
            Instant instant = Instant.parse(startDate);
            ZonedDateTime seoulTime = instant.atZone(ZoneId.of("Asia/Seoul"));

            LocalDate seoulDate = seoulTime.toLocalDate();
            ZonedDateTime newStartDateTime = ZonedDateTime.of(seoulDate, alarmTime, ZoneId.of("Asia/Seoul"));

            startTime = newStartDateTime.format(DateTimeFormatter.ISO_INSTANT);
            endTime = newStartDateTime.plusMinutes(30).format(DateTimeFormatter.ISO_INSTANT);
        } else {
            startTime = formatAlarmTime(alarmTime);
            endTime = formatAlarmTime(alarmTime.plusMinutes(30));
        }

        return Time.builder()
                .startAt(startTime)
                .endAt(endTime)
                .build();
    }

    /**
     * 시간 포매팅 헬퍼 메서드 - ISO 8601 형식으로 변환
     */
    @Override
    public String formatAlarmTime(LocalTime time) {
        // 오늘 시작하도록 설정 (반복 일정의 시작점)
        LocalDate startDate = LocalDate.now();
        LocalDateTime dateTime = LocalDateTime.of(startDate, time);
        
        // 한국 시간대로 ISO 8601 형식 생성
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("Asia/Seoul"));
        String formattedTime = zonedDateTime.format(DateTimeFormatter.ISO_INSTANT);
        
        log.debug("시간 포매팅: LocalTime={} -> ZonedDateTime={} -> ISO={}", 
                time, zonedDateTime, formattedTime);
        
        return formattedTime;
    }

    /**
     * 반복 규칙 생성 헬퍼 메서드
     * authDays 형식: "0101010" (일월화수목금토)
     * 오늘부터 3개월까지 반복 제한
     */
    @Override
    public String buildRecurRule(String authDays) {
        log.debug("RRULE 생성 시작: authDays={}", authDays);
        
        // 카카오 캘린더 API 반복 규칙에 맞게 변환
        StringBuilder rule = new StringBuilder("FREQ=WEEKLY;BYDAY=");
        String[] days = {"SU", "MO", "TU", "WE", "TH", "FR", "SA"};
        
        boolean hasAnyDay = false;
        for (int i = 0; i < authDays.length() && i < 7; i++) {
            if (authDays.charAt(i) == '1') {
                if (hasAnyDay) {
                    rule.append(",");
                }
                rule.append(days[i]);
                hasAnyDay = true;
                log.debug("요일 추가: {} (index={})", days[i], i);
            }
        }
        
        // 만약 어떤 요일도 선택되지 않았다면 기본값 설정 (매일)
        if (!hasAnyDay) {
            rule.append("SU,MO,TU,WE,TH,FR,SA");
            log.debug("기본값 설정: 모든 요일");
        }
        
        // 오늘부터 3개월 후까지 반복 제한 (UNTIL 형식)
        LocalDateTime endDate = LocalDateTime.now().plusMonths(3);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
        String untilDate = endDate.atZone(ZoneId.of("Asia/Seoul"))
                                 .withZoneSameInstant(ZoneOffset.UTC)
                                 .format(formatter);
        rule.append(";UNTIL=").append(untilDate);
        
        String result = rule.toString();
        log.debug("생성된 RRULE: {} (untilDate: {})", result, untilDate);
        return result;
    }

}
