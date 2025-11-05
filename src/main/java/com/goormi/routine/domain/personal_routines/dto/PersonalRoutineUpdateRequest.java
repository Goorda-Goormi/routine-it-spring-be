package com.goormi.routine.domain.personal_routines.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalRoutineUpdateRequest {

    @Size(max = 100)
    private String routineName;

    @Size(max = 10_000)
    private String description;

    @Size(max = 50)
    private String category;

    @Size(max = 255)
    private String goal;

    private LocalTime startTime;

    @Pattern(regexp = "^[01]{7}$", message = "repeatDays는 7자리 0/1 문자열이어야 합니다.")
    private String repeatDays;

    private LocalDate startDate;
    private LocalDate endDate;

    private Boolean isAlarmOn;
    private Boolean isPublic;
}
