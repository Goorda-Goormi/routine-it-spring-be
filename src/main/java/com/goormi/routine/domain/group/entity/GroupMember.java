package com.goormi.routine.domain.group.entity;

import com.goormi.routine.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    private boolean isAlarm;


    @Builder
    private GroupMember(Group group, User user, GroupMemberRole role, GroupMemberStatus status) {
        this.group = group;
        this.user = user;
        this.role = role;
        this.status = status;
        this.isAlarm = group.isAlarm();
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


}
