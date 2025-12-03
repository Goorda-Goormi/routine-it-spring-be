package com.goormi.routine.admin.service;

import com.goormi.routine.admin.dto.BulkDataResponse;
import com.goormi.routine.domain.chat.dto.ChatMessageDto;
import com.goormi.routine.domain.chat.entity.ChatMember;
import com.goormi.routine.domain.chat.entity.ChatMessage;
import com.goormi.routine.domain.chat.entity.ChatRoom;
import com.goormi.routine.domain.chat.repository.ChatMemberRepository;
import com.goormi.routine.domain.chat.repository.ChatMessageRepository;
import com.goormi.routine.domain.chat.repository.ChatRoomRepository;
import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupType;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.notification.entity.Notification;
import com.goormi.routine.domain.notification.entity.NotificationType;
import com.goormi.routine.domain.notification.repository.NotificationRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkDataServiceImpl implements BulkDataService {

	private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationRepository notificationRepository;

	@Override
	@Transactional
	public BulkDataResponse generateUsers(int count) {
		long startTime = System.currentTimeMillis();

		try {
			List<User> users = new ArrayList<>();

			for (int i = 1; i <= count; i++) {
				User user = User.builder()
					.kakaoId("test_kakao_" + i + "_" + System.currentTimeMillis())
					.email("testuser" + i + "@test.com")
					.nickname("테스트유저" + i)
					.profileImageUrl("https://example.com/profile" + i + ".jpg")
					.profileMessage("성능 테스트용 사용자입니다.")
					.role(User.UserRole.USER)
					.active(true)
					.build();

				users.add(user);

				// 배치 사이즈마다 저장 (메모리 효율성)
				if (i % 100 == 0) {
					userRepository.saveAll(users);
					users.clear();
					log.info("Users batch saved: {}/{}", i, count);
				}
			}

			// 남은 사용자들 저장
			if (!users.isEmpty()) {
				userRepository.saveAll(users);
			}

			long executionTime = System.currentTimeMillis() - startTime;
			log.info("Bulk users generation completed: {} users in {}ms", count, executionTime);

			return BulkDataResponse.success("users", count, executionTime);

		} catch (Exception e) {
			log.error("Bulk users generation failed", e);
			return BulkDataResponse.error("users", e.getMessage());
		}
	}

	@Override
	public BulkDataResponse generatePersonalRoutines(int count) {
		// TODO
		return BulkDataResponse.error("personal_routines", "아직 구현되지 않음 - 팀원 2가 구현 예정");
	}

	@Override
	public BulkDataResponse generateGroups(int count) {
        User user = User.builder()
                .kakaoId("test_kakao_@test.leader")
                .email("maybeLeader@111.com")
                .nickname("maybeLeader")
                .role(User.UserRole.USER)
                .active(true)
                .build();

        userRepository.save(user);

        List<Group> groups = new ArrayList<>();
        for (long  i = 1L; i <= count; i++) {
            User leader = userRepository.findById(i).orElse(user);

            if (i > count / 2) {
                Group study = Group.builder()
                        .groupName("group_" + i)
                        .groupType(GroupType.FREE)
                        .description("testgroup_" + i)
                        .leader(leader)
                        .authDays("0101010")
                        .alarmTime(LocalTime.now().plusHours(1))
                        .groupImageUrl("https://example.com/group" + i + ".jpg")
                        .category("study")
                        .maxMembers(20)
                        .build();
                study.setInitialValues(study);
                log.info("created: {}", study.getCreatedAt());
                groups.add(study);
            } else {
                Group living = Group.builder()
                        .groupName("group_" + i)
                        .groupType(GroupType.REQUIRED)
                        .description("testgroup_" + i)
                        .leader(leader)
                        .authDays("0000010")
                        .alarmTime(LocalTime.now().plusHours(1))
                        .groupImageUrl("https://example.com/group" + i + ".jpg")
                        .category("living")
                        .maxMembers(20)
                        .build();
                living.setInitialValues(living);
                groups.add(living);
            }
            // 배치 사이즈마다 저장 (메모리 효율성)
            if (i % 100 == 0) {
                groupRepository.saveAll(groups);
                groups.clear();
                log.info("Users batch saved: {}/{}", i, count);
            }

        }

		return BulkDataResponse.error("groups", "아직 구현되지 않음 - 팀원 2가 구현 예정");
	}

	@Override
	public BulkDataResponse generateChatMessages(int count) {
		// TODO
		return BulkDataResponse.error("chat_messages", "아직 구현되지 않음 - 팀원 1이 구현 예정");
	}

	@Override
	public BulkDataResponse generateChatRooms(int count) {
        User me =  userRepository.findById(1L).orElseThrow(()->new RuntimeException("user not found"));
        userRepository.save(me);
        Group saved = Group.builder()
                .groupName("group_forChat")
                .groupType(GroupType.FREE)
                .description("testgroup_forChat")
                .leader(me)
                .authDays("0101010")
                .alarmTime(LocalTime.now().plusHours(1))
                .groupImageUrl("https://example.com/groupChat.jpg")
                .category("study")
                .maxMembers(20)
                .build();
        saved.setInitialValues(saved);
        groupRepository.save(saved);

        ChatRoom chatRoom = ChatRoom.builder()
                .groupId(saved.getGroupId())
                .roomName(saved.getGroupName())
                .description(saved.getGroupName())
                .maxParticipants(saved.getMaxMembers())
                .isActive(true)
                .createdBy(me.getId())
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 그룹 리더를 채팅방 관리자로 자동 추가
        ChatMember chatMember = ChatMember.builder()
                .roomId(savedChatRoom.getId())
                .userId(me.getId())
                .role(ChatMember.MemberRole.ADMIN)
                .isActive(true)
                .joinedAt(LocalDateTime.now())
                .build();
        chatMemberRepository.save(chatMember);

        List<ChatMessage> messageList = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ChatMessage message = ChatMessage.builder()
                    .roomId(savedChatRoom.getId())
                    .userId(me.getId())
                    .senderNickname(me.getNickname())
                    .message("chattingMESSAGE_"+i)
                    .messageType(ChatMessage.MessageType.TALK)
                    .createdAt(LocalDateTime.now())
                    .build();

            messageList.add(message);

            // 배치 사이즈마다 저장 (메모리 효율성)
            if (i % 100 == 0) {
                chatMessageRepository.saveAll(messageList);
                messageList.clear();
                log.info("Users batch saved: {}/{}", i, count);
            }
        }
		return BulkDataResponse.error("chat_rooms", "아직 구현되지 않음 - 팀원 1이 구현 예정");
	}

	@Override
	public BulkDataResponse generateNotifications(int count) {
        User me =  userRepository.findById(1L)
                .orElseThrow(()->new RuntimeException("user not found"));
        userRepository.save(me);

        List<Notification> notificationList = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Notification notification = Notification.builder()
                    .content("test content" + i)
                    .notificationType(NotificationType.GROUP_TODAY_AUTH_REJECTED)
                    .sender(me)
                    .receiver(me)
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationList.add(notification);
            // 배치 사이즈마다 저장 (메모리 효율성)
            if (i % 100 == 0) {
                notificationRepository.saveAll(notificationList);
                notificationList.clear();
                log.info("notifications batch saved: {}/{}", i, count);
            }
        }

		return BulkDataResponse.error("notifications", "구현중");
	}

	@Override
	@Transactional
	public BulkDataResponse generateAllBulkData(int userCount, int routineCount,
		int groupCount, int messageCount,
		int notificationCount) {
		long startTime = System.currentTimeMillis();
		Map<String, Object> details = new HashMap<>();

		try {
			// 1. 사용자 생성
			log.info("Step 1: Generating {} users", userCount);
			BulkDataResponse userResult = generateUsers(userCount);
			details.put("users", userResult);

			// 2. 루틴 생성
			// log.info("Step 2: Generating {} routines", routineCount);
			// BulkDataResponse routineResult = generatePersonalRoutines(routineCount);
			// details.put("routines", routineResult);

			// 3. 그룹 생성
			// log.info("Step 3: Generating {} groups", groupCount);
			 BulkDataResponse groupResult = generateGroups(groupCount);
			// details.put("groups", groupResult);

			// 4. 채팅 메시지 생성
			// log.info("Step 4: Generating {} chat messages", messageCount);
			// BulkDataResponse messageResult = generateChatMessages(messageCount);
			// details.put("messages", messageResult);

			// 5. 알림 생성
			// log.info("Step 5: Generating {} notifications", notificationCount);
			// BulkDataResponse notificationResult = generateNotifications(notificationCount);
			// details.put("notifications", notificationResult);

			long executionTime = System.currentTimeMillis() - startTime;

			return BulkDataResponse.builder()
				.success(true)
				.message("전체 벌크 데이터 생성 완료 (현재는 사용자만)")
				.dataType("all")
				.generatedCount(userCount) // 현재는 사용자만
				.executionTimeMs(executionTime)
				.details(details)
				.build();

		} catch (Exception e) {
			log.error("Bulk data generation failed", e);
			return BulkDataResponse.error("all", e.getMessage());
		}
	}

	@Override
	@Transactional
	public BulkDataResponse cleanupBulkData() {
		long startTime = System.currentTimeMillis();
		int totalDeleted = 0;

		try {
			// 테스트 사용자들 삭제 (kakaoId가 'test_kakao_'로 시작하는 것들)
			List<User> testUsers = userRepository.findAll()
				.stream()
				.filter(user -> user.getKakaoId().startsWith("test_kakao_"))
				.toList();
			totalDeleted += testUsers.size();
			userRepository.deleteAll(testUsers);

            // 테스트 그룹 삭제 (group_으로 시작하는 이름)
            List<Group> testGroups = groupRepository.findAll()
                    .stream()
                    .filter(group -> group.getGroupName().startsWith("group_"))
                    .toList();
            totalDeleted += testGroups.size();
            groupRepository.deleteAll(testGroups);

            // 테스트 채팅방 삭제 (group_으로 시작하는 이름)
            List<ChatRoom> testChatRooms = chatRoomRepository.findAll()
                    .stream()
                    .filter(chatRoom -> chatRoom.getRoomName().startsWith("group_"))
                    .toList();
            totalDeleted += testChatRooms.size();
            chatRoomRepository.deleteAll(testChatRooms);

            // 테스트 채팅메세지 삭제 chattingMESSAGE_로 시작하는 메세지
            List<ChatMessage> testChatMessages = chatMessageRepository.findAll()
                    .stream()
                    .filter(chatMessage -> chatMessage.getMessage().startsWith("chattingMESSAGE_"))
                    .toList();
            totalDeleted += testChatMessages.size();
            chatMessageRepository.deleteAll(testChatMessages);

            // 테스트 알림 삭제 test로 시작하는 알림
            List<Notification> testNotifications = notificationRepository.findAll()
                    .stream()
                    .filter(notification -> notification.getContent().startsWith("test"))
                    .toList();
            totalDeleted += testNotifications.size();
            notificationRepository.deleteAll(testNotifications);

			long executionTime = System.currentTimeMillis() - startTime;
			log.info("Bulk data cleanup completed: {} items deleted in {}ms", totalDeleted, executionTime);

			return BulkDataResponse.cleanup(totalDeleted, executionTime);

		} catch (Exception e) {
			log.error("Bulk data cleanup failed", e);
			return BulkDataResponse.error("cleanup", e.getMessage());
		}
	}

}