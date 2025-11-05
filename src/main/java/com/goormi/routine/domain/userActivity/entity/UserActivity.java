package com.goormi.routine.domain.userActivity.entity;

import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.personal_routines.domain.PersonalRoutine;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserActivity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private ActivityType activityType;

    private LocalDate activityDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private PersonalRoutine  personalRoutine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private GroupMember groupMember;

    @Column(length = 1000)
    private String imageUrl;
    private Boolean isPublic;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public static UserActivity createActivity (User user,PersonalRoutine personalRoutine, Boolean isPublic) {
        return UserActivity.builder()
                .user(user)
                .personalRoutine(personalRoutine)
                .activityType(ActivityType.PERSONAL_ROUTINE_COMPLETE)
                .activityDate(LocalDate.now(ZoneId.of("Asia/Seoul")))
                .isPublic(isPublic)
                .build();
    }
    public static UserActivity createActivity (User user, GroupMember groupMember, String imageUrl, Boolean isPublic) {
        return UserActivity.builder()
                .user(user)
                .groupMember(groupMember)
                .imageUrl(imageUrl)
                .activityType(ActivityType.GROUP_AUTH_COMPLETE)
                .activityDate(LocalDate.now(ZoneId.of("Asia/Seoul")))
                .isPublic(isPublic)
                .build();
    }

    public void updateActivity(ActivityType activityType, Boolean isPublic) {
        if (activityType != null) this.activityType = activityType;

        if (isPublic != null) this.isPublic = isPublic;

        if (this.activityDate == null) {
            this.activityDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        }

        if (activityType == ActivityType.NOT_COMPLETED) {
            this.activityDate = null;
        }
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

}

