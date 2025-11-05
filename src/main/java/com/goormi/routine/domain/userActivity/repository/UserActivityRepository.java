package com.goormi.routine.domain.userActivity.repository;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.userActivity.entity.ActivityType;
import com.goormi.routine.domain.userActivity.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.EnumSet;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    List<UserActivity> findAllByUserAndActivityDate(User user, LocalDate activityDate);
    Optional<UserActivity>  findByUserAndActivityDateAndActivityType(User user, LocalDate activityDate, ActivityType activityType);
    List<UserActivity> findByGroupMemberInAndActivityTypeAndActivityDate(
            List<GroupMember> groupMembers, ActivityType activityType,
            LocalDate activityDate);
    List<UserActivity> findByGroupMemberAndActivityTypeAndActivityDate(
            GroupMember groupMembers, ActivityType activityType,
            LocalDate activityDate);

    List<UserActivity> findByUserIdAndActivityTypeOrderByCreatedAtDesc(Long userId, ActivityType activityType);
    List<UserActivity> findByUserIdAndImageUrlIsNotNullAndActivityTypeOrderByCreatedAtDesc(Long userId, ActivityType activityType);

    long countByUserIdAndActivityTypeAndCreatedAtBetween(Long userId, ActivityType activityType, LocalDateTime startDate, LocalDateTime endDate);
    List<UserActivity> findByUserIdAndActivityTypeAndActivityDateBetween(Long userId, ActivityType activityType, LocalDate startDate, LocalDate endDate);

    boolean existsByUserIdAndActivityDateAndActivityTypeIn(
            Long userId, LocalDate activityDate, Collection<ActivityType> activityTypes);

    List<UserActivity> findByUserIdAndActivityTypeInAndActivityDateBetween(
            Long userId, List<ActivityType> activityTypes, LocalDate startDate, LocalDate endDate);

    List<UserActivity> findByUserIdAndActivityDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    long countByUserIdAndActivityTypeAndActivityDateBetween(Long userId, ActivityType activityType, LocalDate startDate, LocalDate endDate);

}
