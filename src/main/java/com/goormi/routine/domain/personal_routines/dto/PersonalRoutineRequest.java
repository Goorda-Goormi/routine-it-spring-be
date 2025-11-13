package com.goormi.routine.domain.personal_routines.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalRoutineRequest {

    @NotNull
    private Integer userId;

    @NotBlank
    @Size(max = 100)
    private String routineName;

    @Size(max = 10_000)
    private String description;

    @NotBlank
    @Size(max = 50)
    private String category;   // 추가됨

    @Size(max = 255)
    private String goal;       // 추가됨

    @NotNull
    private LocalTime startTime;

    @NotBlank
    @Pattern(regexp = "^[01]{7}$", message = "repeatDays는 7자리 0/1 문자열이어야 합니다.")
    private String repeatDays;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    private Boolean isAlarmOn = Boolean.TRUE;
    private Boolean isPublic = Boolean.TRUE;
}
