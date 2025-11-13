package com.goormi.routine.domain.notification.service;

import com.goormi.routine.domain.group.dto.request.GroupCreateRequest;
import com.goormi.routine.domain.group.dto.request.GroupJoinRequest;
import com.goormi.routine.domain.group.dto.response.GroupMemberResponse;
import com.goormi.routine.domain.group.dto.response.GroupResponse;
import com.goormi.routine.domain.group.entity.*;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.group.service.GroupMemberService;
import com.goormi.routine.domain.group.service.GroupService;
import com.goormi.routine.domain.notification.dto.NotificationResponse;
import com.goormi.routine.domain.notification.entity.Notification;
import com.goormi.routine.domain.notification.entity.NotificationType;
import com.goormi.routine.domain.notification.repository.NotificationRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.goormi.routine.domain.calendar.service.CalendarIntegrationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("ci")
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupService groupService;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberService groupMemberService;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    
    // 캘린더 통합 서비스를 Mock으로 대체하여 실제 이벤트 처리 방지
    @MockitoBean
    private CalendarIntegrationService calendarIntegrationService;

    private User leader;
    private User user;
    private Group savedGroup;
    private GroupMember savedGroupMember;

    @BeforeEach
    void setUp() {
        // 테스트 용 리더, 멤버 생성
        leader = User.builder()
                .kakaoId("leader")
                .email("testLeader@kakao.com")
                .nickname("testLeader")
                .build();
        userRepository.save(leader);

        user = User.builder()
                .kakaoId("user")
                .email("testUser@kakao.com")
                .nickname("testUser")
                .build();
        userRepository.save(user);

        // 테스트 용 그룹 생성
        GroupCreateRequest groupCreateRequest = GroupCreateRequest.builder()
                .groupName("test")
                .groupDescription("test description")
                .groupType(GroupType.FREE)
                .maxMembers(3)
                .build();

        GroupResponse groupResponse = groupService.createGroup(leader.getId(), groupCreateRequest);
        savedGroup = groupRepository.findById(groupResponse.getGroupId()).orElseThrow();

        // 테스트용 멤버 가입 처리
        GroupJoinRequest joinRequest = GroupJoinRequest.builder()
                .groupId(savedGroup.getGroupId())
                .build();

        GroupMemberResponse joined = groupMemberService.addMember(user.getId(), savedGroup.getGroupId(), joinRequest);
        savedGroupMember = groupMemberRepository.findById(joined.getGroupMemberId()).orElseThrow();
    }

    @Test
    @DisplayName("그룹 가입 요청 알림 생성")
    void createGroupJoinRequestNotification() {
        // given
        NotificationType type = NotificationType.GROUP_JOIN_REQUEST;
        String expectedContent = user.getNickname() + "님이 " + savedGroup.getGroupName() + "에 그룹 가입 요청을 보냈습니다.";

        // when
        NotificationResponse response = notificationService.createNotification(type, user.getId(), leader.getId(), savedGroup.getGroupId());

        // then
        assertThat(response.getContent()).isEqualTo(expectedContent);
        assertThat(response.getNotificationType()).isEqualTo(type);

        List<Notification> notifications = notificationRepository.findByReceiver_IdOrderByCreatedAtDesc(leader.getId());
        assertThat(notifications).hasSize(1);
    }

    @Test
    @DisplayName("수신자 ID로 알림 목록 조회")
    void getNotificationsByReceiver() {
        // given
        notificationRepository.save(Notification.createNotification("content1", NotificationType.GROUP_TODAY_AUTH_COMPLETED, leader, user, savedGroup));
        notificationRepository.save(Notification.createNotification("content2", NotificationType.GROUP_MEMBER_ROLE_UPDATED, leader, user, savedGroup));

        // when
        List<NotificationResponse> responses = notificationService.getNotificationsByReceiver(user.getId());

        // then
        assertThat(responses).hasSize(3); // setUp에서 가입 알림 받음
    }

    @Test
    @DisplayName("알림 타입으로 알림 목록 조회")
    void getNotificationsByNotificationType() {
        // given
        NotificationType type = NotificationType.GROUP_JOIN_REQUEST;
        notificationRepository.save(Notification.createNotification("content1", type, user, leader, savedGroup));
        notificationRepository.save(Notification.createNotification("content2", NotificationType.GROUP_TODAY_AUTH_REQUEST, user, leader, savedGroup));

        // when
        List<NotificationResponse> responses = notificationService.getNotificationsByNotificationType(leader.getId(), type);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getNotificationType()).isEqualTo(type);
    }

    @Test
    @DisplayName("알림 읽음 상태 변경")
    void updateIsRead() {
        // given
        Notification notification = notificationRepository.save(Notification.createNotification
                ("content", NotificationType.GROUP_JOIN_REQUEST, user, leader, savedGroup));
        assertThat(notification.isRead()).isFalse();

        // when
        notificationService.updateIsRead(notification.getId(), leader.getId(), true);

        // then
        Notification updatedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(updatedNotification.isRead()).isTrue();
    }
}
