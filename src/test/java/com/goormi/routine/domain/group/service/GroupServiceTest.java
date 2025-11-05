package com.goormi.routine.domain.group.service;

import com.goormi.routine.domain.group.dto.request.GroupCreateRequest;
import com.goormi.routine.domain.group.dto.request.GroupUpdateRequest;
import com.goormi.routine.domain.group.dto.response.GroupResponse;
import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupType;
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

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("ci")
@Transactional
public class GroupServiceTest {
    @Autowired
    private GroupService groupService;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    
    // 캘린더 통합 서비스를 Mock으로 대체하여 실제 이벤트 처리 방지
    @MockitoBean
    private CalendarIntegrationService calendarIntegrationService;

    private Long leaderId;
    private Group savedGroup;

    @BeforeEach
    void setUp() {
        // 테스트 용 멤버 생성
        User leader = User.builder()
                .kakaoId("leader")
                .email("test@kakao.com")
                .nickname("testUser")
                .build();
        userRepository.save(leader);
        leaderId = leader.getId();

        // 테스트 용 그룹 생성
        GroupCreateRequest request = GroupCreateRequest.builder()
                .groupName("test")
                .groupDescription("test description")
                .groupType(GroupType.FREE)
                .maxMembers(3)
                .isAlarm(true)
                .build();

        GroupResponse response = groupService.createGroup(leader.getId(), request);
        savedGroup = groupRepository.findById(response.getGroupId()).orElseThrow();
    }

    @Test
    @DisplayName("그룹 저장")
    public void createGroup() {
        //given
        Optional<User> user = userRepository.findById(leaderId);
        //when
        //then
        assertThat(savedGroup).isNotNull();
        assertThat(savedGroup.getGroupName()).isEqualTo("test");
        assertThat(savedGroup.getGroupType()).isEqualTo(GroupType.FREE);
        assertThat(savedGroup.getIsAlarm()).isEqualTo(true);
        assertThat(savedGroup.getLeader().getNickname()).isEqualTo(user.get().getNickname());
    }

    @Test
    @DisplayName("가입된 그룹 불러오기")
    public void getJoinedGroups(){
        //when
        List<GroupResponse> joinedGroups = groupService.getJoinedGroups(leaderId);
        List<GroupResponse> groupsByLeaderId = groupService.getGroupsByLeaderId(leaderId);
        //then
        assertThat(joinedGroups.size()).isEqualTo(1);
        assertThat(groupsByLeaderId.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("의무 참여 그룹으로 변경")
    public void updateInfo() {
        //given
        GroupUpdateRequest request = GroupUpdateRequest.builder()
                .groupName("updatedTest")
                .groupType(GroupType.REQUIRED)
                .alarmTime(LocalTime.of(14, 0))
                .authDays("0101010")
                .category("read")
                .build();
        //when
        Long groupId = savedGroup.getGroupId();
        GroupResponse response = groupService.updateGroupInfo(leaderId, groupId, request);
        //then
        assertThat(response.getGroupName()).isEqualTo("updatedTest");
        assertThat(response.getGroupType()).isEqualTo(GroupType.REQUIRED);
        assertThat(response.getAlarmTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.getAuthDays()).isEqualTo("0101010");
        assertThat(response.getCategory()).isEqualTo("read");
    }

    @Test
    @DisplayName("리더가 아닌 경우 수정 오류")
    public void updateInfo_Fail_AccessDenied() {
        //given
        User user = User.builder()
                .kakaoId("user1")
                .email("user@email.com")
                .nickname("justUser")
                .build();

        User saved = userRepository.save(user);
        Long userId = saved.getId();

        GroupUpdateRequest request = GroupUpdateRequest.builder()
                .groupName("updatedTest")
                .groupType(GroupType.REQUIRED)
                .alarmTime(LocalTime.of(14, 0))
                .authDays("0101010")
                .category("read")
                .build();
        //when
        Long groupId = savedGroup.getGroupId();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> groupService.updateGroupInfo(userId, groupId, request));

        //then
        assertThat(exception).isNotNull();
        assertThat(exception.getMessage()).isEqualTo("권한이 없습니다.");
    }

    @Test
    @DisplayName("그룹 삭제 (비활성화)")
    public void deleteGroup() {
        //given
        GroupCreateRequest request = GroupCreateRequest.builder()
                .groupName("forDelete")
                .groupDescription("will be deleted")
                .groupType(GroupType.FREE)
                .maxMembers(3)
                .build();

        GroupResponse created = groupService.createGroup(leaderId, request);
        Long groupId = created.getGroupId();

        //when
        groupService.deleteGroup(leaderId, groupId);
        //then
        assertThat(groupRepository.findAllByIsActive(false)).hasSize(1);
    }
}
