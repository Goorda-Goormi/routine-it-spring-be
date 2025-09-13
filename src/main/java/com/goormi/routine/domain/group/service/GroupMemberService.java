package com.goormi.routine.domain.group.service;

import com.goormi.routine.domain.group.dto.request.GroupJoinRequest;
import com.goormi.routine.domain.group.dto.request.LeaderAnswerRequest;
import com.goormi.routine.domain.group.dto.response.GroupMemberResponse;
import com.goormi.routine.domain.group.entity.GroupMemberRole;
import com.goormi.routine.domain.group.entity.GroupMemberStatus;

import java.util.List;

public interface GroupMemberService {
    // -- Create
    GroupMemberResponse addMember(Long userId, Long groupId, GroupJoinRequest request);

    // -- Read
    List<GroupMemberResponse> getGroupMembersByRole(Long groupId, GroupMemberRole role);
    List<GroupMemberResponse> getGroupMembersByStatus(Long groupId, GroupMemberStatus status);

    GroupMemberResponse getGroupMemberInfo(Long groupId, Long userId);

    // 그룹 멤버들의 인증 미인증 구분을 위함.
    List<GroupMemberResponse> getJoinedGroupMembersWithActivity(Long groupId);

    // -- Update
    GroupMemberResponse updateMemberStatus(Long leaderId, LeaderAnswerRequest request);
    GroupMemberResponse updateMemberRole(Long leaderId, LeaderAnswerRequest request);

    void approveAuthRequest(Long leaderId, Long groupId, LeaderAnswerRequest leaderAnswerRequest);

    void updateIsAlarm(Long groupId, Long userId, boolean isAlarm);

    // -- Delete
    void delete(Long userId, Long groupId);
}
