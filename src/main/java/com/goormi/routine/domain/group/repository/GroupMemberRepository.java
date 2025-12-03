package com.goormi.routine.domain.group.repository;

import com.goormi.routine.domain.group.entity.*;
import com.goormi.routine.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupAndUser(Group group, User user);
    List<GroupMember> findAllByGroupAndRole(Group group, GroupMemberRole role);
    List<GroupMember> findAllByGroupAndStatus(Group group, GroupMemberStatus status);

    List<GroupMember> findAllByUserAndStatus(User user, GroupMemberStatus status);

    @Query("SELECT COUNT(gm) FROM GroupMember gm WHERE gm.group.groupId = :groupId")
    int countMembersByGroupId(@Param("groupId") Long groupId);

    @Query("""
      SELECT COUNT(DISTINCT gm.user.id)
      FROM GroupMember gm
      JOIN UserActivity ua ON gm.user.id = ua.user.id
      WHERE gm.group.groupId = :groupId
        AND gm.status = 'JOINED'
        AND ua.activityType = 'ROUTINE_AUTH'
        AND FUNCTION('DATE_FORMAT', ua.createdAt, '%Y-%m') = :monthYear
      """)
    int countActiveByGroupId(@Param("groupId") Long groupId,
      @Param("monthYear") String monthYear);

    @Query("""
      SELECT COUNT(ua)
      FROM UserActivity ua
      JOIN GroupMember gm ON ua.user.id = gm.user.id
      WHERE gm.group.groupId = :groupId
        AND gm.status = 'JOINED'
        AND ua.activityType = 'ROUTINE_AUTH'
        AND FUNCTION('DATE_FORMAT', ua.createdAt, '%Y-%m') = :monthYear
      """)
    int countAuthByGroupId(@Param("groupId") Long groupId,
      @Param("monthYear") String monthYear);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.user.id = :userId AND gm.status = 'JOINED'")
    List<GroupMember> findActiveGroupsByUserId(@Param("userId") Long userId);

  
    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId AND gm.status = 'JOINED'")
    Optional<GroupMember> findByGroupIdAndUserIdAndIsActiveTrue(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.groupId = :groupId AND gm.status = 'JOINED'")
    List<GroupMember> findAllByGroupId(@Param("groupId") Long groupId);

    @Query("""
    SELECT 
        gm.user.id, 
        CAST(COUNT(gm) AS integer)
    FROM GroupMember gm
    WHERE gm.user.id IN :userIds
      AND gm.status = 'JOINED'
    GROUP BY gm.user.id
    """)
    List<Object[]> countActiveGroupsBatch(@Param("userIds") List<Long> userIds);
}
