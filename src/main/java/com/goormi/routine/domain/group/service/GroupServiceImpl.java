package com.goormi.routine.domain.group.service;

import com.goormi.routine.domain.group.dto.request.GroupCreateRequest;
import com.goormi.routine.domain.group.dto.request.GroupUpdateRequest;
import com.goormi.routine.domain.group.dto.response.GroupResponse;
import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.entity.GroupMemberStatus;
import com.goormi.routine.domain.group.entity.GroupType;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.ranking.entity.Ranking;
import com.goormi.routine.domain.ranking.repository.RankingRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.user.repository.UserRepository;
import com.goormi.routine.domain.chat.entity.ChatRoom;
import com.goormi.routine.domain.chat.entity.ChatMember;
import com.goormi.routine.domain.chat.entity.ChatMember.MemberRole;
import com.goormi.routine.domain.chat.repository.ChatRoomRepository;
import com.goormi.routine.domain.chat.repository.ChatMemberRepository;
import com.goormi.routine.domain.calendar.service.CalendarIntegrationService.GroupInfoUpdateEvent;
import com.goormi.routine.domain.calendar.service.CalendarIntegrationService.GroupDeletionEvent;
import com.goormi.routine.domain.calendar.service.CalendarIntegrationService.GroupMemberStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GroupServiceImpl implements GroupService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final RankingRepository rankingRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    // -- create
    @Override
    public GroupResponse createGroup(Long leaderId, GroupCreateRequest request) {
         User leader = userRepository.findById(leaderId)
                 .orElseThrow(()-> new IllegalArgumentException("User not found"));

        Group group = Group.builder()
                .leader(leader)
                .groupName(request.getGroupName())
                .description(request.getGroupDescription())
                .groupType(request.getGroupType())
                .alarmTime(request.getAlarmTime())
                .authDays(request.getAuthDays())
                .category(request.getCategory())
                .groupImageUrl(request.getImageUrl())
                .maxMembers(request.getMaxMembers())
                .isAlarm(request.getIsAlarm())
                .build();

        group.addLeader(leader);
        group.setInitialValues(group);
        Group saved = groupRepository.save(group);

        // 리더 추가에 대한 캘린더 연동 이벤트 발행
        // 리더는 그룹 생성과 동시에 JOINED 상태가 되므로 이벤트 발행 필요
        GroupMember leaderMember = groupMemberRepository.findByGroupAndUser(saved, leader)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));
        if (leaderMember.getStatus() == GroupMemberStatus.JOINED) {
            log.info("그룹 생성 시 리더에 대한 캘린더 이벤트 발행: groupId={}, leaderId={}",
                    saved.getGroupId(), leader.getId());
            applicationEventPublisher.publishEvent(new GroupMemberStatusChangeEvent(leaderMember));
        }

        
        // 그룹 생성 시 자동으로 채팅방 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .groupId(saved.getGroupId())
                .roomName(saved.getGroupName())
                .description(saved.getGroupName())
                .maxParticipants(saved.getMaxMembers())
                .isActive(true)
                .createdBy(leader.getId())
                .build();
        
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        
        // 그룹 리더를 채팅방 관리자로 자동 추가
        ChatMember chatMember = ChatMember.builder()
                .roomId(savedChatRoom.getId())
                .userId(leader.getId())
                .role(MemberRole.ADMIN)
                .isActive(true)
                .build();
        
        chatMemberRepository.save(chatMember);
        
        return GroupResponse.from(saved);
    }

    // -- Read
    @Override
    @Transactional(readOnly = true)
    public GroupResponse getGroupInfo(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(groupId + " is not found"));

        return GroupResponse.from(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsByLeaderId(Long leaderId){
        List<Group> groups = groupRepository.findAllByLeaderId(leaderId);
        return groups.stream()
                .filter(Group::isActive)
                .map(GroupResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsWithFiltering(GroupType groupType, String category){
        List<GroupResponse> responses;
        if (groupType != null && category != null) {
            List<GroupResponse> byType = getGroupsByGroupType(groupType);
            List<GroupResponse> byCategory = getGroupsByCategory(category);
            Set<GroupResponse> categorySet = new HashSet<>(byCategory);

            responses = byType.stream().filter(categorySet::contains).toList();
        } else if (groupType != null) {
            responses = getGroupsByGroupType(groupType);
        } else if (category != null) {
            responses = getGroupsByCategory(category);
        } else {
            // 기본적으로는 활성화된 그룹만 가져오도록 처리.
            responses = getGroupsByIsActive(true);
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsByGroupType(GroupType groupType){
        List<Group> groups = groupRepository.findAllByGroupType(groupType);
        return  groups.stream()
                .filter(Group::isActive)
                .map(GroupResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsByCategory(String category){
        List<Group> groups = groupRepository.findAllByCategory(category);
        return  groups.stream()
                .filter(Group::isActive)
                .map(GroupResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> getGroupsByIsActive(boolean isActive){
        List<Group> groups = groupRepository.findAllByIsActive(isActive);
        return groups.stream()
                .map(GroupResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponse> getJoinedGroups(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<GroupMember> memberList = groupMemberRepository.findAllByUserAndStatus(user, GroupMemberStatus.JOINED);

        return memberList.stream()
                .map(GroupMember::getGroup)
                .filter(Group::isActive)
                .map(GroupResponse::from)
                .toList();
    }



    // -- Update
    @Override
    public GroupResponse updateGroupInfo(Long leaderId, Long groupId, GroupUpdateRequest request) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(groupId + " is not found"));

        if (!Objects.equals(leaderId, group.getLeader().getId())){
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        group.updateBasicInfo(request.getGroupName(), request.getGroupDescription(), request.getGroupType());
        group.updateTimeInfo(request.getAlarmTime(), request.getAuthDays());
        group.updateOtherInfo(request.getCategory(), request.getImageUrl(), request.getMaxMembers());
        if(request.getIsAlarm() != null) group.updateIsAlarm(request.getIsAlarm());

        // 캘린더 연동을 위한 이벤트 발행
        log.info("그룹 정보 변경 이벤트 발행: groupId={}, groupName={}", groupId, group.getGroupName());
        applicationEventPublisher.publishEvent(new GroupInfoUpdateEvent(group));

        return GroupResponse.from(group);
    }

    // -- Delete
    @Override
    public void deleteGroup(Long leaderId, Long groupId){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(groupId + " is not found"));

        if (!Objects.equals(leaderId, group.getLeader().getId())){
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        
        // 캘린더 연동을 위한 이벤트 발행 (비활성화 전에 발행)
        log.info("그룹 삭제 이벤트 발행: groupId={}, groupName={}", groupId, group.getGroupName());
        applicationEventPublisher.publishEvent(new GroupDeletionEvent(group));

        List<Ranking> groupRankings = rankingRepository.findAllByGroupId(groupId);
        if (!groupRankings.isEmpty()) {
            rankingRepository.deleteAllByGroupId(groupId);
            log.info("그룹 {} 관련 랭킹 데이터 삭제 완료 ({}개)", groupId, groupRankings.size());
        }
        
        group.deactivate(); // 비활성화 후 일정기간 후 삭제?
//        groupRepository.delete(group);
    }

}
