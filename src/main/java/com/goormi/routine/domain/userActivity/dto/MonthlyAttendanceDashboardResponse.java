package com.goormi.routine.domain.userActivity.dto;

import com.goormi.routine.domain.userActivity.entity.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class MonthlyAttendanceDashboardResponse {

    @Schema(description = "요약 정보")
    private Summary summary;

    @Schema(description = "달력 형태 일자별 출석 정보 (1일~말일)")
    private List<AttendanceDayDto> calendar;

    @Getter
    @Builder
    public static class Summary {
        @Schema(description = "해당 월의 총 일수")
        private int totalDays;

        @Schema(description = "출석 인정 일수")
        private int attendedDays;

        @Schema(description = "출석률 (0.0~100.0, 소수 1자리)")
        private double attendanceRate;

        @Schema(description = "해당 월 내 최장 연속 출석 일수")
        private int longestStreak;

        @Schema(description = "해당 월 내 현재 연속 출석 일수")
        private int currentStreak;

        @Schema(description = "타입별 집계 - 개인루틴 완료 수")
        private int personalRoutineCount;

        @Schema(description = "타입별 집계 - 그룹 인증 완료 수")
        private int groupAuthCount;

        @Schema(description = "체크리스트 기록 수 (참고용)")
        private int dailyChecklistCount;
    }

    @Getter
    @Builder
    public static class AttendanceDayDto {
        @Schema(description = "일자")
        private LocalDate date;

        @Schema(description = "출석 여부")
        private boolean attended;

        @Schema(description = "해당 일자의 활동 타입 집합")
        private Set<ActivityType> activityTypes;
    }
}
