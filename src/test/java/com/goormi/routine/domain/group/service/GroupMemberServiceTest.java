package com.goormi.routine.domain.group.service;

import com.goormi.routine.domain.group.dto.request.GroupCreateRequest;
import com.goormi.routine.domain.group.dto.request.GroupJoinRequest;
import com.goormi.routine.domain.group.dto.request.LeaderAnswerRequest;
import com.goormi.routine.domain.group.dto.response.GroupMemberResponse;
import com.goormi.routine.domain.group.dto.response.GroupResponse;
import com.goormi.routine.domain.group.entity.*;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import com.goormi.routine.domain.calendar.service.CalendarIntegrationService;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("ci")
@Transactional
public class GroupMemberServiceTest {

    @Autowired
    private GroupService groupService;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberService groupMemberService;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private UserRepository userRepository;
    
    // 캘린더 통합 서비스를 Mock으로 대체하여 실제 이벤트 처리 방지
    @MockitoBean
    private CalendarIntegrationService calendarIntegrationService;

    private Long leaderId;
    private Long userId;
    private Group savedGroup;
    private GroupMember savedGroupMember;

    @BeforeEach
    void setUp() {
        // 테스트 용 리더, 멤버 생성
        User leader = User.builder()
                .kakaoId("leader")
                .email("testLeader@kakao.com")
                .nickname("testLeader")
                .build();
        userRepository.save(leader);
        leaderId = leader.getId();

        User user = User.builder()
                .kakaoId("user")
                .email("testUser@kakao.com")
                .nickname("testUser")
                .build();
        userRepository.save(user);
        userId = user.getId();

        // 테스트 용 그룹 생성
        GroupCreateRequest groupCreateRequest = GroupCreateRequest.builder()
                .groupName("test")
                .groupDescription("test description")
                .groupType(GroupType.REQUIRED)
                .maxMembers(3)
                .isAlarm(false)
                .build();

        GroupResponse groupResponse = groupService.createGroup(leader.getId(), groupCreateRequest);
        savedGroup = groupRepository.findById(groupResponse.getGroupId()).orElseThrow();

        // 테스트용 멤버 가입 처리
        GroupJoinRequest joinRequest = GroupJoinRequest.builder()
                .groupId(savedGroup.getGroupId())
                .build();

        GroupMemberResponse joined = groupMemberService.addMember(userId, savedGroup.getGroupId(), joinRequest);
        savedGroupMember = groupMemberRepository.findById(joined.getGroupMemberId()).orElseThrow();
    }

    @Test
    @DisplayName("그룹가입신청")
    public void addMember_success() {
        //given
        User newUser = User.builder()
                .kakaoId("newUser")
                .email("testNewUser@kakao.com")
                .nickname("testNewUser").build();
        userRepository.save(newUser);

        GroupJoinRequest request = GroupJoinRequest.builder().
                groupId(savedGroup.getGroupId()).build();

        // when
        GroupMemberResponse added = groupMemberService.addMember(newUser.getId(), savedGroup.getGroupId(), request);

        //then
        assertThat(added.getGroupName()).isEqualTo(savedGroup.getGroupName());
        assertThat(added.getStatus()).isEqualTo(GroupMemberStatus.PENDING);
        assertThat(added.getRole()).isEqualTo(GroupMemberRole.MEMBER);
        assertThat(savedGroup.getGroupMembers()).hasSize(3);
        assertThat(savedGroup.getCurrentMemberCnt()).isEqualTo(1);
    }
    @Test
    @DisplayName("지유그룹가입신청")
    public void addMember_FREE_success() {
        //given
        GroupCreateRequest groupCreateRequest = GroupCreateRequest.builder()
                .groupName("FREEGroup")
                .groupDescription("addFREEGroup")
                .groupType(GroupType.FREE)
                .maxMembers(3)
                .build();

        GroupResponse groupResponse = groupService.createGroup(leaderId, groupCreateRequest);
        Group freeGroup = groupRepository.findById(groupResponse.getGroupId()).orElseThrow();

        GroupJoinRequest request = GroupJoinRequest.builder().
                groupId(freeGroup.getGroupId()).build();

        // when
        GroupMemberResponse added = groupMemberService.addMember(userId, freeGroup.getGroupId(), request);

        //then
        assertThat(added.getGroupName()).isEqualTo(freeGroup.getGroupName());
        assertThat(added.getStatus()).isEqualTo(GroupMemberStatus.JOINED);
        assertThat(added.getRole()).isEqualTo(GroupMemberRole.MEMBER);
        assertThat(freeGroup.getGroupMembers()).hasSize(2);
        assertThat(freeGroup.getCurrentMemberCnt()).isEqualTo(2);
    }
    @Test
    @DisplayName("이미 있는 경우 그룹가입신청 실패")
    public void addMember_fail() {
        //given
        GroupJoinRequest request = GroupJoinRequest.builder()
                .groupId(savedGroup.getGroupId())
                .groupMemberId(leaderId)
                .build();
        //when & then
        assertThatThrownBy(() -> groupMemberService.addMember(leaderId, savedGroup.getGroupId(), request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("이미 참여 중이거나 대기 중입니다.");
    }

    @Test
    @DisplayName("그룹멤버의 알람 변경")
    public void updateIsAlarm() {
        //when
        groupMemberService.updateIsAlarm(savedGroup.getGroupId(), userId, true);
        //then
        assertThat(savedGroupMember.getIsAlarm()).isEqualTo(true);
    }

    @Test
    @DisplayName("상태 업데이트")
    public void updateMember_success() {
        //given
        LeaderAnswerRequest request = LeaderAnswerRequest.builder()
                .groupId(savedGroup.getGroupId())
                .leaderId(leaderId)
                .targetMemberId(savedGroupMember.getMemberId())
                .status(GroupMemberStatus.JOINED)
                .build();
        //when
        GroupMemberResponse updated = groupMemberService.updateMemberStatus(leaderId, request);

        //then
        assertThat(updated.getGroupName()).isEqualTo("test");
        assertThat(updated.getStatus()).isEqualTo(GroupMemberStatus.JOINED);
        assertThat(savedGroupMember.getStatus()).isEqualTo(GroupMemberStatus.JOINED);
        assertThat(savedGroup.getCurrentMemberCnt()).isEqualTo(2);
    }
    @Test
    @DisplayName("리더 위임")
    public void updateMemberRole_success() {
        //given
        LeaderAnswerRequest requestStatus = LeaderAnswerRequest.builder()
                .groupId(savedGroup.getGroupId())
                .leaderId(leaderId)
                .targetMemberId(savedGroupMember.getMemberId())
                .status(GroupMemberStatus.JOINED)
                .build();
        LeaderAnswerRequest requestRole = LeaderAnswerRequest.builder()
                .groupId(savedGroup.getGroupId())
                .leaderId(leaderId)
                .targetMemberId(savedGroupMember.getMemberId())
                .role(GroupMemberRole.LEADER)
                .build();

        //when
        groupMemberService.updateMemberStatus(leaderId, requestStatus);
        GroupMemberResponse updatedMemberRole = groupMemberService.updateMemberRole(leaderId, requestRole);

        //then
        assertThat(updatedMemberRole.getGroupName()).isEqualTo("test");
        assertThat(updatedMemberRole.getRole()).isEqualTo(GroupMemberRole.LEADER);
        assertThat(savedGroupMember.getRole()).isEqualTo(GroupMemberRole.LEADER);
        assertThat(savedGroup.getLeader().getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("그룹 탈퇴")
    public void delete() {
        //given
        User leftUser = User.builder()
                .kakaoId("leftUser")
                .email("testLeftUser@kakao.com")
                .nickname("testLeftUser").build();
        userRepository.save(leftUser);
        GroupJoinRequest request = GroupJoinRequest.builder().
                groupId(savedGroup.getGroupId()).build();

        GroupMemberResponse added = groupMemberService.addMember(leftUser.getId(), savedGroup.getGroupId(), request);
        GroupMember leftGroupMember = groupMemberRepository.findById(added.getGroupMemberId()).orElseThrow();
        //when
        groupMemberService.delete(leftUser.getId(), savedGroup.getGroupId());
        //then
        assertThat(leftGroupMember.getStatus()).isEqualTo(GroupMemberStatus.LEFT);
        assertThat(savedGroup.getCurrentMemberCnt()).isEqualTo(1);
    }

}
