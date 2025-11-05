package com.goormi.routine.domain.personal_routines.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalRoutineResponse {
    private Integer routineId;
    private Integer userId;
    private String routineName;
    private String description;
    private String category;
    private String goal;
    private LocalTime startTime;
    private String repeatDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isAlarmOn;
    private Boolean isPublic;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
