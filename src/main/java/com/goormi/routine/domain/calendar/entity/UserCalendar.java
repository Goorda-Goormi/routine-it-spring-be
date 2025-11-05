package com.goormi.routine.domain.calendar.entity;

import com.goormi.routine.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자별 카카오 서브캘린더 정보를 관리하는 엔티티
 */
@Entity
@Table(name = "user_calendar")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_calendar_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "sub_calendar_id", nullable = false, unique = true)
    private String subCalendarId;

    @Column(name = "calendar_name", nullable = false)
    private String calendarName;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private CalendarColor color;

    @Column(name = "reminder_minutes", nullable = false)
    private Integer reminderMinutes;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private UserCalendar(User user, String subCalendarId, String calendarName, 
                        CalendarColor color, Integer reminderMinutes) {
        this.user = user;
        this.subCalendarId = subCalendarId;
        this.calendarName = calendarName;
        this.color = color;
        this.reminderMinutes = reminderMinutes;
        this.active = true; // 기본값
    }

    public static UserCalendar createUserCalendar(User user, String subCalendarId) {
        return UserCalendar.builder()
                .user(user)
                .subCalendarId(subCalendarId)
                .calendarName("routine-it for group")
                .color(CalendarColor.LIME)
                .reminderMinutes(10)
                .active(true)  // 명시적으로 active 설정
                .build();
    }

  
    public void updateCalendarId(String newCalendarId) {
        this.subCalendarId = newCalendarId;
    }

  
    public void deactivate() {
        this.active = false;
    }

    /**
     * 비즈니스 메서드: 캘린더 활성화
     */
    public void activate() {
        this.active = true;
    }

    /**
     * 캘린더 색상 Enum
     */
    public enum CalendarColor {
        RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE, PINK, LIME, BROWN, GRAY
    }
}
