package com.goormi.routine.domain.group.service;

import com.goormi.routine.domain.group.dto.request.GroupJoinRequest;
import com.goormi.routine.domain.group.dto.request.LeaderAnswerRequest;
import com.goormi.routine.domain.group.dto.response.GroupMemberResponse;
import com.goormi.routine.domain.group.entity.*;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.notification.entity.NotificationType;
import com.goormi.routine.domain.notification.service.NotificationService;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import com.goormi.routine.domain.userActivity.entity.ActivityType;
import com.goormi.routine.domain.userActivity.entity.UserActivity;
import com.goormi.routine.domain.userActivity.repository.UserActivityRepository;
import com.goormi.routine.domain.chat.entity.ChatRoom;
import com.goormi.routine.domain.chat.entity.ChatMember;
import com.goormi.routine.domain.chat.entity.ChatMember.MemberRole;
import com.goormi.routine.domain.chat.repository.ChatRoomRepository;
import com.goormi.routine.domain.chat.repository.ChatMemberRepository;
import com.goormi.routine.domain.chat.service.ChatService;
import com.goormi.routine.domain.userActivity.dto.UserActivityRequest;
import com.goormi.routine.domain.userActivity.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupMemberServiceImpl implements GroupMemberService {
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final NotificationService notificationService;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatService chatService;

    private final UserActivityService userActivityService;


    // 그룹에 멤버가 참여 신청시 펜딩으로 추가
    @Override
    public GroupMemberResponse addMember(Long userId, Long groupId, GroupJoinRequest request) {

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(()->new IllegalArgumentException("Group not found"));

        if(!Objects.equals(groupId, request.getGroupId())){
            throw new IllegalArgumentException("Invalid group id");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(()->new IllegalArgumentException("User not found"));

        Optional<GroupMember> existingMember = groupMemberRepository
                .findByGroupAndUser(group, user);

        if (existingMember.isPresent()) {
            GroupMember member = existingMember.get();
            if (member.getStatus() == GroupMemberStatus.BLOCKED) {
                throw new IllegalArgumentException("BLOCKED Member");
            }
            if (member.getStatus() == GroupMemberStatus.JOINED ||
                    member.getStatus() == GroupMemberStatus.PENDING) {
                throw new IllegalArgumentException("이미 참여 중이거나 대기 중입니다.");
            }
        }

        GroupMember groupMember = group.addMember(user); // PENDING
        if (group.getGroupType() == GroupType.REQUIRED){
            // 리더에게 가입 신청
            notificationService.createNotification(NotificationType.GROUP_JOIN_REQUEST,
                    userId, group.getLeader().getId(), group.getGroupId());
        }
        // 자유 참여는 바로 가입 처리
        if(group.getGroupType() == GroupType.FREE){
            groupMember.changeStatus(GroupMemberStatus.JOINED);
            group.addMemberCnt();

            // 유저에게 가입됨을 알림
            notificationService.createNotification(NotificationType.GROUP_MEMBER_STATUS_UPDATED,
                    group.getLeader().getId(),userId, group.getGroupId());

            // 채팅방 자동 참여
            ChatRoom chatRoom = chatRoomRepository.findFirstByGroupIdAndIsActiveTrue(group.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Chat room not found for group"));

            // 이미 채팅방 멤버인지 확인
            Optional<ChatMember> existingChatMember = chatMemberRepository
                    .findByRoomIdAndUserId(chatRoom.getId(), userId);

            if (existingChatMember.isEmpty()) {
                ChatMember chatMember = ChatMember.builder()
                        .roomId(chatRoom.getId())
                        .userId(userId)
                        .role(MemberRole.MEMBER)
                        .isActive(true)
                        .build();
                chatMemberRepository.save(chatMember);
                
                // 채팅방에 그룹 멤버 가입 알림 전송
                chatService.notifyMemberJoin(chatRoom.getId(), userId);
            }
        }
        groupMemberRepository.save(groupMember);


        return GroupMemberResponse.from(groupMember);
    }

    // -- Read
    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembersByRole(Long groupId, GroupMemberRole role) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(()->new IllegalArgumentException("Group not found"));

        List<GroupMember> groupMembers = groupMemberRepository.findAllByGroupAndRole(group, role);
        return groupMembers.stream()
                .map(GroupMemberResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getGroupMembersByStatus(Long groupId, GroupMemberStatus status) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(()->new IllegalArgumentException("Group not found"));

        List<GroupMember> groupMembers = groupMemberRepository.findAllByGroupAndStatus(group, status);
        return groupMembers.stream()
                .map(GroupMemberResponse::from)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public GroupMemberResponse getGroupMemberInfo(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(()->new IllegalArgumentException("Group not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(()->new IllegalArgumentException("User not found"));

        GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(()->new IllegalArgumentException("GroupMember not found"));

        List<UserActivity> completedActivities = userActivityRepository
                .findByGroupMemberAndActivityTypeAndActivityDate(groupMember, ActivityType.GROUP_AUTH_COMPLETE, LocalDate.now());

        boolean isAuthToday = !completedActivities.isEmpty();
        return GroupMemberResponse.from(groupMember, isAuthToday);
    }

    // 그룹 멤버들의 인증 미인증 구분을 위함.
    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getJoinedGroupMembersWithActivity(Long groupId){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(()->new IllegalArgumentException("Group not found"));
        List<GroupMember> groupMembers = groupMemberRepository.findAllByGroupAndStatus(group, GroupMemberStatus.JOINED);
        List<UserActivity> completedActivities = userActivityRepository.
                findByGroupMemberInAndActivityTypeAndActivityDate(groupMembers, ActivityType.GROUP_AUTH_COMPLETE, LocalDate.now());

        Set<Long> completedMemberIds = completedActivities.stream()
                .map(activity -> activity.getGroupMember().getMemberId())
                .collect(Collectors.toSet());

        return groupMembers.stream()
                .map(member -> {
                    boolean isAuthToday = completedMemberIds.contains(member.getMemberId());
                    return GroupMemberResponse.from(member, isAuthToday);
                }).toList();
    }


    // -- Update
    @Override
    public GroupMemberResponse updateMemberStatus(Long leaderId, LeaderAnswerRequest request) {
        Group group = validateLeader(leaderId, request);
        GroupMember groupMember = validateMember(request);

        GroupMemberStatus oldStatus = groupMember.getStatus();
        GroupMemberStatus newStatus = request.getStatus();

        if (oldStatus == GroupMemberStatus.PENDING) {
            if (newStatus != GroupMemberStatus.JOINED && newStatus != GroupMemberStatus.LEFT) {
                throw new IllegalArgumentException("JOINED, LEFT만 가능합니다");
            }
            if (newStatus == GroupMemberStatus.JOINED) {
                group.addMemberCnt(); // 가입 시 인원 수 증가

                // 채팅방 자동 참여
                ChatRoom chatRoom = chatRoomRepository.findFirstByGroupIdAndIsActiveTrue(group.getGroupId())
                        .orElseThrow(() -> new IllegalArgumentException("Chat room not found for group"));

                // 이미 채팅방 멤버인지 확인
                Optional<ChatMember> existingChatMember = chatMemberRepository
                        .findByRoomIdAndUserId(chatRoom.getId(), groupMember.getUser().getId());

                if (existingChatMember.isEmpty()) {
                    ChatMember chatMember = ChatMember.builder()
                            .roomId(chatRoom.getId())
                            .userId(groupMember.getUser().getId())
                            .role(MemberRole.MEMBER)
                            .isActive(true)
                            .build();
                    chatMemberRepository.save(chatMember);
                    
                    // 채팅방에 그룹 멤버 가입 알림 전송
                    chatService.notifyMemberJoin(chatRoom.getId(), groupMember.getUser().getId());
                }
            }
        }
        else if (oldStatus == GroupMemberStatus.JOINED) {
            if (newStatus != GroupMemberStatus.BLOCKED && newStatus != GroupMemberStatus.LEFT) {
                throw new IllegalArgumentException("BLOCKED, LEFT만 가능합니다");
            }
            group.minusMemberCnt(); // 차단, 탈퇴 시 인원 수 감소

            // 채팅방에서 자동 탈퇴
            ChatRoom chatRoom = chatRoomRepository.findFirstByGroupIdAndIsActiveTrue(group.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Chat room not found for group"));

            Optional<ChatMember> existingChatMember = chatMemberRepository
                    .findByRoomIdAndUserId(chatRoom.getId(), groupMember.getUser().getId());

            if (existingChatMember.isPresent()) {
                ChatMember chatMember = existingChatMember.get();
                chatMember.setIsActive(false);
                chatMemberRepository.save(chatMember);
                
                // 채팅방에 그룹 멤버 탈퇴 알림 전송
                chatService.notifyMemberLeave(chatRoom.getId(), groupMember.getUser().getId());
            }
        }
        else if (oldStatus == GroupMemberStatus.BLOCKED ){
            if (newStatus != GroupMemberStatus.PENDING) { // 차단 풀기
                throw new IllegalArgumentException("PENDING 만 가능합니다.");
            }
        }
        groupMember.changeStatus(newStatus); // JOINED, BLOCKED, LEFT
        // 유저에게 역할 변경 알림
        notificationService.createNotification(NotificationType.GROUP_MEMBER_STATUS_UPDATED,
                group.getLeader().getId(), groupMember.getUser().getId(), group.getGroupId());
        return GroupMemberResponse.from(groupMember);
    }

    @Override
    public GroupMemberResponse updateMemberRole(Long leaderId, LeaderAnswerRequest request) {
        Group group = validateLeader(leaderId, request);
        GroupMember targetGroupMember = validateMember(request);

        // JOINED Member만 리더 가능
        if(targetGroupMember.getStatus() != GroupMemberStatus.JOINED){
            throw new IllegalArgumentException("가입된 그룹멤버가 아님");
        }

        GroupMemberRole oldRole = targetGroupMember.getRole();
        GroupMemberRole newRole = request.getRole();

        if (newRole == oldRole) {
            throw new IllegalArgumentException("같은 역할 입니다.");
        }
        if (newRole == GroupMemberRole.MEMBER) {
            throw new IllegalArgumentException("리더를 위임하세요");
        }

        // 리더 위임시 멤버 -> 리더, 리더 -> 멤버
        User user = targetGroupMember.getUser();
        User leader = group.getLeader();
        group.changeLeader(user);

        GroupMember groupLeader = groupMemberRepository.findByGroupAndUser(group, leader)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));
        groupLeader.changeRole(GroupMemberRole.MEMBER);
        targetGroupMember.changeRole(request.getRole()); // LEADER, MEMBER

        notificationService.createNotification(NotificationType.GROUP_MEMBER_ROLE_UPDATED,
                group.getLeader().getId(), targetGroupMember.getUser().getId(), group.getGroupId());

        return GroupMemberResponse.from(targetGroupMember);
    }


    // 리더 승인을 받아 그룹 루틴 완료 처리
    @Override
    public void approveAuthRequest(Long leaderId, Long groupId, LeaderAnswerRequest leaderAnswerRequest){
        Group group = validateLeader(leaderId, leaderAnswerRequest);
        if (!Objects.equals(groupId, group.getGroupId())) {
            throw new IllegalArgumentException("not equal group id");
        }
        GroupMember groupMember = validateMember(leaderAnswerRequest);

        UserActivityRequest activityRequest = UserActivityRequest.builder()
                .groupId(group.getGroupId())
                .activityType(ActivityType.GROUP_AUTH_COMPLETE)
                .activityDate(leaderAnswerRequest.getActivityDate())
                .imageUrl(leaderAnswerRequest.getImageUrl())
                .build();

        if (leaderAnswerRequest.isApproved()) {
            userActivityService.create(groupMember.getUser().getId(), activityRequest);
            notificationService.createNotification(NotificationType.GROUP_TODAY_AUTH_COMPLETED,
                    group.getLeader().getId(), groupMember.getUser().getId(), group.getGroupId());
        }
        else {
            notificationService.createNotification(NotificationType.GROUP_TODAY_AUTH_REJECTED,
                    group.getLeader().getId(), groupMember.getUser().getId(), group.getGroupId());
        }
    }

    private Group validateLeader(Long leaderId, LeaderAnswerRequest request) {
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(()->new IllegalArgumentException("Group not found"));

        if(!Objects.equals(leaderId, group.getLeader().getId())){
            throw new IllegalArgumentException(" is not the leader of this member");
        }
        return group;
    }

    private GroupMember validateMember(LeaderAnswerRequest request) {
        GroupMember groupMember = groupMemberRepository.findById(request.getTargetMemberId())
                .orElseThrow(()->new IllegalArgumentException("Member not found"));

        if (!Objects.equals(groupMember.getGroup().getGroupId(), request.getGroupId())) {
            throw new IllegalArgumentException("해당 그룹의 멤버가 아님");
        }
        return groupMember;
    }

    @Override
    public void updateIsAlarm(Long groupId, Long userId, boolean isAlarm) {
        Group group = groupRepository.findById(groupId).orElseThrow(()->new IllegalArgumentException("Group not found"));
        User user = userRepository.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found"));
        GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, user).orElseThrow(()->new IllegalArgumentException("Member not found"));

        if (isAlarm == groupMember.isAlarm()) {
            return;
        }
        groupMember.changeIsAlarm(isAlarm);
    }

    // -- Delete
    @Override
    public void delete(Long userId, Long groupId) { // 본인이 탈퇴하는 것, 리더는 블락 사용
        Group group = groupRepository.findById(groupId).
                orElseThrow(()->new IllegalArgumentException("Group not found"));

        User user = userRepository.findById(userId).
                orElseThrow(()->new IllegalArgumentException("User not found"));

        GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, user)
                .orElseThrow(()-> new IllegalArgumentException("Group Member not found"));

        if (groupMember.getRole() == GroupMemberRole.LEADER) {
            throw new IllegalArgumentException("리더는 떠날 수 없습니다. 리더를 위임해주세요");
        }
        if (groupMember.getStatus() == GroupMemberStatus.BLOCKED) {
            throw new IllegalArgumentException("차단된 멤버는 떠날 수 없습니다.");
        }
        groupMember.changeStatus(GroupMemberStatus.LEFT);

        // 채팅방에서 자동 탈퇴
        ChatRoom chatRoom = chatRoomRepository.findFirstByGroupIdAndIsActiveTrue(group.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found for group"));

        Optional<ChatMember> existingChatMember = chatMemberRepository
                .findByRoomIdAndUserId(chatRoom.getId(), userId);

        if (existingChatMember.isPresent()) {
            ChatMember chatMember = existingChatMember.get();
            chatMember.setIsActive(false);
            chatMember.setLeftAt(java.time.LocalDateTime.now());
            chatMemberRepository.save(chatMember);
            
            // 채팅방에 그룹 멤버 탈퇴 알림 전송
            chatService.notifyMemberLeave(chatRoom.getId(), userId);
        }

//        groupMemberRepository.delete(groupMember);
    }
}
