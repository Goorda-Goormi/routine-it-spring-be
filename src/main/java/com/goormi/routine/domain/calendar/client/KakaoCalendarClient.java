package com.goormi.routine.domain.calendar.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goormi.routine.domain.calendar.dto.KakaoCalendarDto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * 카카오 캘린더 API 호출 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
public class KakaoCalendarClient {

    private final WebClient webClient;


    /**
     * 서브캘린더 생성
     * 
     * @param accessToken 카카오 액세스 토큰
     * @param request 서브캘린더 생성 요청
     * @return 생성된 서브캘린더 정보
     */
    public CreateSubCalendarResponse createSubCalendar(String accessToken, CreateSubCalendarRequest request) {
        log.info("=== 카카오 서브캘린더 생성 API 호출 시작 ===");
        log.debug("카카오 서브캘린더 생성 요청: name={}, color={}", request.name(), request.color());
        log.debug("Access Token 존재 여부: {}", accessToken != null && !accessToken.trim().isEmpty());
        
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("name", request.name());
            formData.add("color", request.color());
            if (request.reminder() != null) {
                formData.add("reminder", request.reminder().toString());
            }
            
            return webClient.post()
                    .uri("/create/calendar")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8")
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(CreateSubCalendarResponse.class)
                    .doOnSuccess(response -> log.info("서브캘린더 생성 성공: subCalendarId={}", response.subCalendarId()))
                    .doOnError(error -> log.error("서브캘린더 생성 실패", error))
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("카카오 API 호출 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("서브캘린더 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 서브캘린더 삭제
     * 
     * @param accessToken 카카오 액세스 토큰
     * @param subCalendarId 삭제할 서브캘린더 ID
     */
    public void deleteSubCalendar(String accessToken, String subCalendarId) {
        log.info("=== 카카오 서브캘린더 삭제 API 호출 시작 ===");
        log.debug("카카오 서브캘린더 삭제 요청: subCalendarId={}", subCalendarId);
        log.debug("Access Token 존재 여부: {}", accessToken != null && !accessToken.trim().isEmpty());
        
        try {
            webClient.delete()
                    .uri( "/delete/calendar?calendar_id=" + subCalendarId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnSuccess(response -> log.info("서브캘린더 삭제 성공: subCalendarId={}", subCalendarId))
                    .doOnError(error -> log.error("서브캘린더 삭제 실패: subCalendarId={}", subCalendarId, error))
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("카카오 API 호출 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("서브캘린더 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 서브캘린더 목록 조회
     * 
     * @param accessToken 카카오 액세스 토큰
     * @return 서브캘린더 목록
     */
    public GetCalendarsResponse getCalendars(String accessToken) {
        log.info("=== 카카오 서브캘린더 목록 조회 API 호출 시작 ===");
        log.debug("Access Token 존재 여부: {}", accessToken != null && !accessToken.trim().isEmpty());
        
        try {
            return webClient.get()
                    .uri("/calendars")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("카카오 API 오류 응답: status={}, body={}", 
                                            response.statusCode(), body))
                                    .then(response.createException()))
                    .bodyToMono(GetCalendarsResponse.class)
                    .doOnSuccess(response -> log.info("서브캘린더 목록 조회 성공: {} 개", 
                            response.calendars() != null ? response.calendars().length : 0))
                    .doOnError(error -> log.error("서브캘린더 목록 조회 실패", error))
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("카카오 API 호출 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("서브캘린더 목록 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 일정 생성
     * 
     * @param accessToken 카카오 액세스 토큰
     * @param subCalendarId 서브캘린더 ID
     * @param request 일정 생성 요청
     * @return 생성된 일정 정보
     */
    public CreateEventResponse createEvent(String accessToken, String subCalendarId, CreateEventRequest request) {
        log.debug("카카오 일정 생성 요청: title={}, subCalendarId={}", 
                request.event() != null ? request.event().title() : "null", subCalendarId);
        
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            
            // calendar_id 추가
            formData.add("calendar_id", request.calendarId());
            
            // request.event() null 체크
            if (request.event() == null) {
                log.error("request.event()가 null입니다!");
                throw new IllegalArgumentException("event 객체가 null입니다");
            }
            
            // JSON 형태로 event 데이터 변환 (event 객체만)
            ObjectMapper objectMapper = new ObjectMapper();
            String eventJson;
            try {
                eventJson = objectMapper.writeValueAsString(request.event());
                log.debug("JSON 직렬화 성공: {}", eventJson);
            } catch (Exception e) {
                log.error("JSON 직렬화 실패", e);
                throw new RuntimeException("JSON 직렬화 실패", e);
            }
            
            // eventJson이 비어있는지 확인
            if (eventJson == null || eventJson.trim().isEmpty() || eventJson.equals("-")) {
                log.error("eventJson이 비어있거나 잘못되었습니다: '{}'", eventJson);
                throw new IllegalArgumentException("eventJson이 유효하지 않습니다: " + eventJson);
            }
            
            formData.add("event", eventJson);
            
            log.debug("일정 생성 요청 데이터:");
            log.debug("- event JSON: {}", eventJson);
            formData.forEach((key, values) -> 
                log.debug("- form data: {} = {}", key, values));
            
            return webClient.post()
                    .uri( "/create/event")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8")
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("카카오 API 오류 응답: status={}, body={}", 
                                            response.statusCode(), body))
                                    .then(response.createException()))
                    .bodyToMono(CreateEventResponse.class)
                    .doOnSuccess(response -> log.info("일정 생성 성공: eventId={}", response.eventId()))
                    .doOnError(error -> log.error("일정 생성 실패", error))
                    .block();
                    
        } catch (Exception e) {
            log.error("카카오 API 호출 오류", e);
            throw new RuntimeException("일정 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 일정 수정
     * 
     * @param accessToken 카카오 액세스 토큰
     * @param eventId 수정할 일정 ID
     * @param request 일정 수정 요청
     */
    public void updateEvent(String accessToken, String eventId, UpdateEventRequest request) {
        log.info("=== 카카오 일정 수정 API 호출 시작 ===");
        log.debug("요청 파라미터: eventId={}, title={}", eventId, 
                request.event() != null ? request.event().title() : "null");
        log.debug("UpdateEventRequest: eventId={}, calendarId={}, recurUpdateType={}", 
                request.eventId(), request.calendarId(), request.recurUpdateType());
        
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            
            // UpdateEventRequest의 eventId 사용 (있으면 우선, 없으면 파라미터 사용)
            String targetEventId = request.eventId() != null ? request.eventId() : eventId;
            formData.add("event_id", targetEventId);
            log.debug("Target Event ID: {}", targetEventId);
            
            // recurUpdateType 추가
            if (request.recurUpdateType() != null) {
                formData.add("recur_update_type", request.recurUpdateType());
                log.debug("Recur Update Type: {}", request.recurUpdateType());
            }
            
            // calendarId 추가 (있는 경우)
            if (request.calendarId() != null) {
                formData.add("calendar_id", request.calendarId());
                log.debug("Calendar ID: {}", request.calendarId());
            }
            
            // event 객체 null 체크
            if (request.event() == null) {
                log.error("UpdateEventRequest.event가 null입니다!");
                throw new IllegalArgumentException("event 객체가 null입니다");
            }
            
            // event 객체 필드값 검증 및 로깅
            EventUpdate eventUpdate = request.event();
            log.debug("Event Update 필드값:");
            log.debug("- title: {}", eventUpdate.title());
            log.debug("- description: {}", eventUpdate.description());
            log.debug("- rrule: {}", eventUpdate.rrule());
            
            if (eventUpdate.time() != null) {
                log.debug("- time.startAt: {}", eventUpdate.time().startAt());
                log.debug("- time.endAt: {}", eventUpdate.time().endAt());
            } else {
                log.error("Time 객체가 null입니다!");
            }
            
            // JSON 형태로 event 데이터 변환
            ObjectMapper objectMapper = new ObjectMapper();
            String eventJson;
            try {
                eventJson = objectMapper.writeValueAsString(request.event());
                log.debug("JSON 직렬화 성공: {}", eventJson);
            } catch (Exception e) {
                log.error("JSON 직렬화 실패", e);
                throw new RuntimeException("JSON 직렬화 실패", e);
            }
            
            // eventJson이 비어있는지 확인
            if (eventJson == null || eventJson.trim().isEmpty() || eventJson.equals("{}")) {
                log.error("eventJson이 비어있거나 잘못되었습니다: '{}'", eventJson);
                throw new IllegalArgumentException("eventJson이 유효하지 않습니다: " + eventJson);
            }
            
            formData.add("event", eventJson);
            
            // 최종 form data 로깅
            log.debug("최종 Form Data:");
            formData.forEach((key, values) -> 
                log.debug("- {}: {}", key, values));
            
            webClient.post()
                    .uri("/update/event/host")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE + ";charset=UTF-8")
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .doOnNext(body -> log.error("카카오 API 오류 응답: status={}, body={}", 
                                            response.statusCode(), body))
                                    .then(response.createException()))
                    .bodyToMono(Void.class)
                    .doOnSuccess(response -> log.info("일정 수정 성공: eventId={}", eventId))
                    .doOnError(error -> log.error("일정 수정 실패: eventId={}", eventId, error))
                    .block();
                    
        } catch (Exception e) {
            log.error("카카오 API 호출 오류: eventId={}", eventId, e);
            throw new RuntimeException("일정 수정에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 일정 삭제
     *
     * @param accessToken 카카오 액세스 토큰
     * @param request     일정 삭제 요청
     */
    public void deleteEvent(String accessToken, DeleteEventRequest request) {
        log.debug("카카오 일정 삭제 요청: eventId={}, recurUpdateType={}",
                request.eventId(), request.recurUpdateType());

        try {
            webClient.delete()
                    .uri(uriBuilder -> uriBuilder.path("/delete/event")
                            .queryParam("event_id", request.eventId())
                            .queryParam("recur_update_type", request.recurUpdateType())
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnSuccess(response -> log.info("일정 삭제 성공: eventId={}", request.eventId()))
                    .doOnError(error -> log.error("일정 삭제 실패: eventId={}", request.eventId(), error))
                    .block();

        } catch (WebClientResponseException e) {
            log.error("카카오 API 호출 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("일정 삭제에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 일정 조회
     * @return 일정 목록
     */
    public GetEventsResponse getEvents(String accessToken, GetEventsRequest request) {
        log.info("=== 카카오 일정 조회 API 호출 시작 ===");
        log.debug("조회 파라미터: {}", request);

        try {
            return webClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/events")
                                .queryParam("calendar_id", request.calendarId())
                                .queryParam("from",  request.from())
                                .queryParam("to",  request.to());
                        return uriBuilder.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .doOnNext(body ->
                                            log.error("카카오 API 오류 응답: status={}, body={}",
                                            response.statusCode(), body))
                                    .then(response.createException()))
                    .bodyToMono(GetEventsResponse.class)
                    .doOnSuccess(response -> log.info("일정 조회 성공: 조회된 일정 수={}",
                            response.events() != null ? response.events().length : 0))
                    .doOnError(error -> log.error("일정 조회 실패: calendarId={}", request.calendarId(), error))
                    .block();

        } catch (WebClientResponseException e) {
            log.error("카카오 API 호출 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("일정 조회에 실패했습니다: " + e.getMessage(), e);
        }
    }
}
