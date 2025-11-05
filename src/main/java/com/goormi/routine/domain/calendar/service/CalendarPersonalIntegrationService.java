package com.goormi.routine.domain.calendar.service;

import com.goormi.routine.domain.personal_routines.domain.PersonalRoutine;
import com.goormi.routine.domain.personal_routines.repository.PersonalRoutineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

/**
 * 개인 루틴과 캘린더 간의 연동을 담당하는 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
public class CalendarPersonalIntegrationService {

    private final CalendarPersonalService calendarPersonalService;
    private final CalendarService calendarService;
    private final PersonalRoutineRepository personalRoutineRepository;

    @PostConstruct
    public void postConstruct() {
        log.info("CalendarPersonalIntegrationService initialized successfully.");
    }

    /**
     * 개인 루틴 생성 시 캘린더 일정 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePersonalRoutineCreation(PersonalRoutineCreatedEvent event) {
        PersonalRoutine personalRoutine = event.getPersonalRoutine();
        Long userId = personalRoutine.getUserId().longValue();
        
        log.info("개인 루틴 생성 이벤트 수신: userId={}, routineId={}, routineName={}",
                userId, personalRoutine.getRoutineId(), personalRoutine.getRoutineName());

        try {
            String eventId = calendarPersonalService.createPersonalSchedule(userId, personalRoutine);

            // 생성된 이벤트 ID를 개인 루틴에 저장
            personalRoutine.updateCalendarEventId(eventId);
            personalRoutineRepository.save(personalRoutine);
            log.info("개인 루틴 캘린더 일정 생성 완료: userId={}, routineId={}, eventId={}",
                    userId, personalRoutine.getRoutineId(), eventId);

        } catch (Exception e) {
            log.error("개인 루틴 일정 생성 실패: userId={}, routineId={}",
                    userId, personalRoutine.getRoutineId(), e);
        }
    }

    /**
     * 개인 루틴 수정 시 캘린더 일정 업데이트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePersonalRoutineUpdate(PersonalRoutineUpdatedEvent event) {
        PersonalRoutine personalRoutine = event.getPersonalRoutine();
        Long userId = personalRoutine.getUserId().longValue();
        String eventId = personalRoutine.getCalendarEventId();
        
        log.info("개인 루틴 수정 이벤트 수신: userId={}, routineId={}, eventId={}",
                userId, personalRoutine.getRoutineId(), eventId);

        try {
            // 이벤트 ID가 존재하는 경우에만 일정 수정
            if (eventId != null && !eventId.trim().isEmpty()) {
                calendarPersonalService.updatePersonalSchedule(userId, personalRoutine, eventId);
                log.info("개인 루틴 캘린더 일정 수정 완료: userId={}, routineId={}, eventId={}",
                        userId, personalRoutine.getRoutineId(), eventId);
            } else {
                log.warn("eventId is null :eventId={}", eventId);
            }
        } catch (Exception e) {
            log.error("개인 루틴 일정 수정 실패: userId={}, routineId={}, eventId={}",
                    userId, personalRoutine.getRoutineId(), eventId, e);
        }
    }

    /**
     * 개인 루틴 삭제 시 캘린더 일정 삭제
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePersonalRoutineDeletion(PersonalRoutineDeletedEvent event) {
        PersonalRoutine personalRoutine = event.getPersonalRoutine();
        Long userId = personalRoutine.getUserId().longValue();
        String eventId = personalRoutine.getCalendarEventId();
        
        log.info("개인 루틴 삭제 이벤트 수신: userId={}, routineId={}, eventId={}",
                userId, personalRoutine.getRoutineId(), eventId);

        try {
            // 이벤트 ID가 존재하는 경우에만 일정 삭제
            if (eventId != null && !eventId.trim().isEmpty()) {
                calendarPersonalService.deletePersonalSchedule(eventId, userId);
                
                // 삭제 후 이벤트 ID 제거
                personalRoutine.clearCalendarEventId();
                log.info("개인 루틴 캘린더 일정 삭제 완료: userId={}, routineId={}, eventId={}",
                        userId, personalRoutine.getRoutineId(), eventId);
            } else {
                log.warn("캘린더 연동 안됨 또는 이벤트 ID 없음 - 일정 삭제 스킵: userId={}, eventId={}", 
                        userId, eventId);
            }
        } catch (Exception e) {
            log.error("개인 루틴 일정 삭제 실패: userId={}, routineId={}, eventId={}",
                    userId, personalRoutine.getRoutineId(), eventId, e);
        }
    }

    /**
     * 개인 루틴 이벤트 클래스들
     */
    public static class PersonalRoutineCreatedEvent {
        private final PersonalRoutine personalRoutine;

        public PersonalRoutineCreatedEvent(PersonalRoutine personalRoutine) {
            this.personalRoutine = personalRoutine;
        }

        public PersonalRoutine getPersonalRoutine() { 
            return personalRoutine; 
        }
    }

    public static class PersonalRoutineUpdatedEvent {
        private final PersonalRoutine personalRoutine;

        public PersonalRoutineUpdatedEvent(PersonalRoutine personalRoutine) {
            this.personalRoutine = personalRoutine;
        }

        public PersonalRoutine getPersonalRoutine() { 
            return personalRoutine; 
        }
    }

    public static class PersonalRoutineDeletedEvent {
        private final PersonalRoutine personalRoutine;

        public PersonalRoutineDeletedEvent(PersonalRoutine personalRoutine) {
            this.personalRoutine = personalRoutine;
        }

        public PersonalRoutine getPersonalRoutine() { 
            return personalRoutine; 
        }
    }
}