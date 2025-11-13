package com.goormi.routine.domain.personal_routines.service;

import com.goormi.routine.domain.personal_routines.domain.PersonalRoutine;
import com.goormi.routine.domain.personal_routines.dto.PersonalRoutineRequest;
import com.goormi.routine.domain.personal_routines.dto.PersonalRoutineResponse;
import com.goormi.routine.domain.personal_routines.dto.PersonalRoutineUpdateRequest;
import com.goormi.routine.domain.personal_routines.repository.PersonalRoutineRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.goormi.routine.domain.calendar.service.CalendarPersonalIntegrationService.*;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalRoutineService {

    private final PersonalRoutineRepository repository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public PersonalRoutineResponse create(PersonalRoutineRequest req) {
        validateDateRange(req.getStartDate(), req.getEndDate());

        if (repository.existsByUserIdAndRoutineNameAndIsDeletedFalse(req.getUserId(), req.getRoutineName())) {
            throw new IllegalArgumentException("동일한 이름의 루틴이 이미 존재합니다.");
        }

        PersonalRoutine entity = PersonalRoutine.builder()
                .userId(req.getUserId())
                .routineName(req.getRoutineName())
                .description(req.getDescription())
                .category(req.getCategory())
                .goal(req.getGoal())
                .startTime(req.getStartTime())
                .repeatDays(req.getRepeatDays())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isAlarmOn(Boolean.TRUE.equals(req.getIsAlarmOn()))
                .isPublic(Boolean.TRUE.equals(req.getIsPublic()))
                .isDeleted(false)
                .build();

        PersonalRoutine savedEntity = repository.save(entity);
        
        // 개인 루틴 생성 이벤트 발행
        applicationEventPublisher.publishEvent(new PersonalRoutineCreatedEvent(savedEntity));
        
        return toResponse(savedEntity);
    }

    @Transactional(readOnly = true)
    public List<PersonalRoutineResponse> listByUser(Integer userId) {
        return repository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PersonalRoutineResponse get(Integer routineId) {
        PersonalRoutine entity = repository.findByRoutineIdAndIsDeletedFalse(routineId)
                .orElseThrow(() -> new EntityNotFoundException("루틴을 찾을 수 없습니다."));
        return toResponse(entity);
    }

    public PersonalRoutineResponse update(Integer routineId, PersonalRoutineUpdateRequest req) {
        PersonalRoutine entity = repository.findByRoutineIdAndIsDeletedFalse(routineId)
                .orElseThrow(() -> new EntityNotFoundException("루틴을 찾을 수 없습니다."));

        if (req.getRoutineName() != null) entity.setRoutineName(req.getRoutineName());
        if (req.getDescription() != null) entity.setDescription(req.getDescription());
        if (req.getCategory() != null) entity.setCategory(req.getCategory());
        if (req.getGoal() != null) entity.setGoal(req.getGoal());
        if (req.getStartTime() != null) entity.setStartTime(req.getStartTime());
        if (req.getRepeatDays() != null) entity.setRepeatDays(req.getRepeatDays());
        if (req.getStartDate() != null) entity.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) entity.setEndDate(req.getEndDate());
        if (req.getIsAlarmOn() != null) entity.setIsAlarmOn(req.getIsAlarmOn());
        if (req.getIsPublic() != null) entity.setIsPublic(req.getIsPublic());

        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            validateDateRange(entity.getStartDate(), entity.getEndDate());
        }

        // 개인 루틴 수정 이벤트 발행
        applicationEventPublisher.publishEvent(new PersonalRoutineUpdatedEvent(entity));

        return toResponse(entity); // flush on commit
    }

    public void softDelete(Integer routineId) {
        PersonalRoutine entity = repository.findByRoutineIdAndIsDeletedFalse(routineId)
                .orElseThrow(() -> new EntityNotFoundException("루틴을 찾을 수 없습니다."));
        
        // 개인 루틴 삭제 이벤트 발행 (소프트 삭제 전에 발행)
        applicationEventPublisher.publishEvent(new PersonalRoutineDeletedEvent(entity));
        
        entity.softDelete();
    }

    public PersonalRoutineResponse toggleAlarm(Integer routineId) {
        PersonalRoutine entity = repository.findByRoutineIdAndIsDeletedFalse(routineId)
                .orElseThrow(() -> new EntityNotFoundException("루틴을 찾을 수 없습니다."));
        entity.toggleAlarm();
        applicationEventPublisher.publishEvent(new PersonalRoutineUpdatedEvent(entity));
        return toResponse(entity);
    }

    public PersonalRoutineResponse togglePublic(Integer routineId) {
        PersonalRoutine entity = repository.findByRoutineIdAndIsDeletedFalse(routineId)
                .orElseThrow(() -> new EntityNotFoundException("루틴을 찾을 수 없습니다."));
        entity.togglePublic();
        return toResponse(entity);
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDate는 startDate보다 빠를 수 없습니다.");
        }
    }

    private PersonalRoutineResponse toResponse(PersonalRoutine e) {
        return PersonalRoutineResponse.builder()
                .routineId(e.getRoutineId())
                .userId(e.getUserId())
                .routineName(e.getRoutineName())
                .description(e.getDescription())
                .category(e.getCategory())
                .goal(e.getGoal())
                .startTime(e.getStartTime())
                .repeatDays(e.getRepeatDays())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .isAlarmOn(e.getIsAlarmOn())
                .isPublic(e.getIsPublic())
                .isDeleted(e.getIsDeleted())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
