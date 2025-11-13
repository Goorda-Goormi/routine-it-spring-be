package com.goormi.routine.domain.group.entity;

import com.goormi.routine.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Slf4j
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupMemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupMemberRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean isAlarm;

    @Column(name = "calendar_event_id", length = 1024)
    private String calendarEventId; // 카카오 캘린더 이벤트 ID


    @Builder
    private GroupMember(Group group, User user, GroupMemberRole role, GroupMemberStatus status) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.status = status;
        this.isAlarm = group.getIsAlarm();
    }

    public static GroupMember createGroupMember (Group group, User user, GroupMemberRole role, GroupMemberStatus status) {
        GroupMember groupMember = GroupMember.builder()
                .group(group)
                .user(user)
                .role(role)
                .status(status)
                .build();

        groupMember.createdAt = LocalDateTime.now();
        groupMember.updatedAt = groupMember.createdAt;

        return groupMember;
    }

    public void changeRole(GroupMemberRole role) {
        this.role = role;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeStatus(GroupMemberStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void changeIsAlarm(boolean isAlarm) {
        this.isAlarm = isAlarm;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 캘린더 이벤트 ID 업데이트 (그룹 가입 시 호출)
     */
    public void updateCalendarEventId(String eventId) {
        this.calendarEventId = eventId;
        this.updatedAt = LocalDateTime.now();
        log.debug("캘린더 이벤트 ID 업데이트 eventId: {}", eventId);
    }

    /**
     * 캘린더 이벤트 ID 제거 (그룹 탈퇴 시 호출)
     */
    public void clearCalendarEventId() {
        this.calendarEventId = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 캘린더 이벤트가 연동되어 있는지 확인
     */
    public boolean hasCalendarEvent() {
        return this.calendarEventId != null && !this.calendarEventId.trim().isEmpty();
    }


}
