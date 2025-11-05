package com.goormi.routine.domain.personal_routines.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "personal_routines")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "routine_id")
    private Integer routineId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "routine_name", length = 100, nullable = false)
    private String routineName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50, nullable = false)
    private String category;   //카테고리 추가

    @Column(name = "goal", length = 255)
    private String goal;       //목표 추가

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * 월~일 7자리 0/1 (예: 1010100 -> 월/수/금)
     */
    @Column(name = "repeat_days", length = 7, nullable = false)
    private String repeatDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_alarm_on", nullable = false)
    private Boolean isAlarmOn = Boolean.TRUE;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = Boolean.TRUE;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "calendar_event_id", length = 1024)
    private String calendarEventId;

    // 도메인 편의 메서드
    public void softDelete() { this.isDeleted = true; }
    public void toggleAlarm() { this.isAlarmOn = !this.isAlarmOn; }
    public void togglePublic() { this.isPublic = !this.isPublic; }

    public void updateCalendarEventId(String calendarEventId) {
        this.calendarEventId = calendarEventId;
    }
    public void clearCalendarEventId() {
        this.calendarEventId = null;
    }
}
