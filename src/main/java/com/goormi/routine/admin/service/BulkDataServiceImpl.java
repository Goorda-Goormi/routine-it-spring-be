package com.goormi.routine.admin.service;

import com.goormi.routine.admin.dto.BulkDataResponse;
import com.goormi.routine.domain.chat.entity.ChatMember;
import com.goormi.routine.domain.chat.entity.ChatMessage;
import com.goormi.routine.domain.chat.entity.ChatRoom;
import com.goormi.routine.domain.chat.repository.ChatMemberRepository;
import com.goormi.routine.domain.chat.repository.ChatMessageRepository;
import com.goormi.routine.domain.chat.repository.ChatRoomRepository;
import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupType;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional
	public BulkDataResponse generateGroups(int count) {
		long startTime = System.currentTimeMillis();

		try {
			// 기존 사용자들 조회 (리더로 사용)
			List<User> users = userRepository.findAll();
			if (users.isEmpty()) {
				// 기본 사용자가 없으면 하나 생성
				User defaultUser = User.builder()
					.kakaoId("default_leader_" + System.currentTimeMillis())
					.email("default@leader.com")
					.nickname("기본리더")
					.role(User.UserRole.USER)
					.active(true)
					.build();
				users.add(userRepository.save(defaultUser));
			}

			List<Group> groups = new ArrayList<>();

			for (int i = 1; i <= count; i++) {
				// 리더를 순환적으로 할당
				User leader = users.get((i - 1) % users.size());

				Group group;
				if (i > count / 2) {
					group = Group.builder()
						.groupName("테스트스터디그룹_" + i)
						.groupType(GroupType.FREE)
						.description("테스트용 스터디 그룹_" + i)
						.leader(leader)
						.authDays("0101010")
						.alarmTime(LocalTime.of(9, 0))
						.groupImageUrl("https://example.com/group" + i + ".jpg")
						.category("study")
						.maxMembers(20)
						.build();
				} else {
					group = Group.builder()
						.groupName("테스트생활그룹_" + i)
						.groupType(GroupType.REQUIRED)
						.description("테스트용 생활 그룹_" + i)
						.leader(leader)
						.authDays("0000010")
						.alarmTime(LocalTime.of(21, 0))
						.groupImageUrl("https://example.com/group" + i + ".jpg")
						.category("living")
						.maxMembers(20)
						.build();
				}

				// setInitialValues 호출 (group이 null이 아님을 보장)
				group.setInitialValues(group);
				groups.add(group);

				// 배치 사이즈마다 저장 (메모리 효율성)
				if (i % 100 == 0) {
					groupRepository.saveAll(groups);
					groups.clear();
					log.info("Groups batch saved: {}/{}", i, count);
				}
			}

			// 남은 그룹들 저장
			if (!groups.isEmpty()) {
				groupRepository.saveAll(groups);
			}

			long executionTime = System.currentTimeMillis() - startTime;
			log.info("Bulk groups generation completed: {} groups in {}ms", count, executionTime);

			return BulkDataResponse.success("groups", count, executionTime);

		} catch (Exception e) {
			log.error("Bulk groups generation failed", e);
			return BulkDataResponse.error("groups", e.getMessage());
		}
	}

	@Override
	@Transactional
	public BulkDataResponse generateChatRooms(int count) {
		long startTime = System.currentTimeMillis();

		try {
			// 기존 그룹들 조회
			List<Group> groups = groupRepository.findAll();
			if (groups.isEmpty()) {
				throw new RuntimeException("그룹이 없습니다. 먼저 그룹을 생성하세요.");
			}

			List<ChatRoom> chatRooms = new ArrayList<>();

			for (int i = 1; i <= count; i++) {
				// 그룹을 순환적으로 할당
				Group group = groups.get((i - 1) % groups.size());

				ChatRoom chatRoom = ChatRoom.builder()
					.groupId(group.getGroupId())
					.roomName("테스트채팅방_" + i)
					.description("테스트용 채팅방_" + i)
					.maxParticipants(group.getMaxMembers())
					.isActive(true)
					.createdBy(group.getLeader().getId())
					.build();

				chatRooms.add(chatRoom);

				// 배치 사이즈마다 저장 (메모리 효율성)
				if (i % 100 == 0) {
					List<ChatRoom> savedRooms = chatRoomRepository.saveAll(chatRooms);

					// 각 채팅방에 리더를 관리자로 추가
					List<ChatMember> chatMembers = new ArrayList<>();
					for (ChatRoom savedRoom : savedRooms) {
						Group roomGroup = groups.stream()
							.filter(g -> g.getGroupId().equals(savedRoom.getGroupId()))
							.findFirst()
							.orElse(group);

						ChatMember chatMember = ChatMember.builder()
							.roomId(savedRoom.getId())
							.userId(roomGroup.getLeader().getId())
							.role(ChatMember.MemberRole.ADMIN)
							.isActive(true)
							.build();
						chatMembers.add(chatMember);
					}
					chatMemberRepository.saveAll(chatMembers);

					chatRooms.clear();
					log.info("ChatRooms batch saved: {}/{}", i, count);
				}
			}

			// 남은 채팅방들 저장
			if (!chatRooms.isEmpty()) {
				List<ChatRoom> savedRooms = chatRoomRepository.saveAll(chatRooms);

				// 각 채팅방에 리더를 관리자로 추가
				List<ChatMember> chatMembers = new ArrayList<>();
				for (ChatRoom savedRoom : savedRooms) {
					Group roomGroup = groups.stream()
						.filter(g -> g.getGroupId().equals(savedRoom.getGroupId()))
						.findFirst()
						.orElse(groups.get(0));

					ChatMember chatMember = ChatMember.builder()
						.roomId(savedRoom.getId())
						.userId(roomGroup.getLeader().getId())
						.role(ChatMember.MemberRole.ADMIN)
						.isActive(true)
						.build();
					chatMembers.add(chatMember);
				}
				chatMemberRepository.saveAll(chatMembers);
			}

			long executionTime = System.currentTimeMillis() - startTime;
			log.info("Bulk chat rooms generation completed: {} rooms in {}ms", count, executionTime);

			return BulkDataResponse.success("chat_rooms", count, executionTime);

		} catch (Exception e) {
			log.error("Bulk chat rooms generation failed", e);
			return BulkDataResponse.error("chat_rooms", e.getMessage());
		}
	}


	@Override
	public BulkDataResponse generateChatMessages(int count) {
		long startTime = System.currentTimeMillis();

		try {
			// 기존 채팅방들 조회
			List<ChatRoom> chatRooms = chatRoomRepository.findAll();
			if (chatRooms.isEmpty()) {
				throw new RuntimeException("채팅방이 없습니다. 먼저 채팅방을 생성하세요.");
			}

			// 기존 사용자들 조회
			List<User> users = userRepository.findAll();
			if (users.isEmpty()) {
				throw new RuntimeException("사용자가 없습니다. 먼저 사용자를 생성하세요.");
			}

			List<ChatMessage> messages = new ArrayList<>();
			String[] sampleMessages = {
				"안녕하세요!",
				"오늘 루틴 완료했습니다!",
				"화이팅!",
				"좋은 하루 되세요",
				"목표 달성했어요",
				"함께 해서 좋네요",
				"열심히 하고 있습니다",
				"응원해주세요",
				"감사합니다",
				"내일도 화이팅!",
				"성공적인 하루였어요",
				"잘 부탁드립니다",
				"오늘도 수고하셨어요",
				"좋은 결과 있기를 바라요",
				"모두 함께 파이팅!"
			};

			for (int i = 1; i <= count; i++) {
				// 랜덤하게 채팅방과 사용자 선택
				ChatRoom randomRoom = chatRooms.get(ThreadLocalRandom.current().nextInt(chatRooms.size()));
				User randomUser = users.get(ThreadLocalRandom.current().nextInt(users.size()));
				String randomMessage = sampleMessages[ThreadLocalRandom.current().nextInt(sampleMessages.length)];

				ChatMessage message = ChatMessage.builder()
					.roomId(randomRoom.getId())
					.userId(randomUser.getId())
					.senderNickname(randomUser.getNickname())
					.message(randomMessage + "_" + i)
					.messageType(ChatMessage.MessageType.TALK)
					.build();

				messages.add(message);

				// 배치 저장
				if (i % 100 == 0) {
					chatMessageRepository.saveAll(messages);
					messages.clear();
					log.info("Messages batch saved: {}/{}", i, count);
				}
			}

			// 남은 메시지들 저장
			if (!messages.isEmpty()) {
				chatMessageRepository.saveAll(messages);
			}

			long executionTime = System.currentTimeMillis() - startTime;
			log.info("Bulk chat messages generation completed: {} messages in {}ms", count, executionTime);

			return BulkDataResponse.success("chat_messages", count, executionTime);

		} catch (Exception e) {
			log.error("Bulk chat messages generation failed", e);
			return BulkDataResponse.error("chat_messages", e.getMessage());
		}
	}

	@Override
	@Transactional
	public BulkDataResponse generateAllBulkData(int userCount,
		int groupCount, int messageCount) {
		long startTime = System.currentTimeMillis();
		Map<String, Object> details = new HashMap<>();

		try {
			BulkDataResponse userResult = generateUsers(userCount);
			BulkDataResponse groupResult = generateGroups(groupCount);
			BulkDataResponse chatRoomResult = generateChatRooms(groupCount);
			BulkDataResponse messageResult = generateChatMessages(messageCount);

			long executionTime = System.currentTimeMillis() - startTime;
			int totalGenerated = userCount + groupCount * 2 +
				(messageCount > 0 ? messageCount : 0);


			return BulkDataResponse.builder()
				.success(true)
				.message("전체 벌크 데이터 생성 완료")
				.dataType("all")
				.generatedCount(totalGenerated)
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
			// 1. 테스트 채팅 메시지 삭제
			List<ChatMessage> testMessages = chatMessageRepository.findAll()
				.stream()
				.filter(msg -> msg.getMessage().contains("_") &&
					(msg.getMessage().contains("안녕하세요") ||
						msg.getMessage().contains("루틴 완료") ||
						msg.getMessage().contains("화이팅")))
				.toList();

			totalDeleted += testMessages.size();
			chatMessageRepository.deleteAll(testMessages);
			log.info("테스트 채팅 메시지 {} 개 삭제", testMessages.size());

			// 2. 테스트 채팅방 멤버 삭제
			List<ChatRoom> testChatRooms = chatRoomRepository.findAll()
				.stream()
				.filter(room -> room.getRoomName().startsWith("테스트채팅방_"))
				.toList();

			for (ChatRoom room : testChatRooms) {
				List<ChatMember> members = chatMemberRepository.findAll()
					.stream()
					.filter(member -> member.getRoomId().equals(room.getId()))
					.toList();
				chatMemberRepository.deleteAll(members);
				totalDeleted += members.size();
			}

			// 3. 테스트 채팅방 삭제
			totalDeleted += testChatRooms.size();
			chatRoomRepository.deleteAll(testChatRooms);
			log.info("테스트 채팅방 {} 개 삭제", testChatRooms.size());

			// 4. 테스트 그룹 삭제
			List<Group> testGroups = groupRepository.findAll()
				.stream()
				.filter(group -> group.getGroupName().startsWith("테스트스터디그룹_") ||
					group.getGroupName().startsWith("테스트생활그룹_"))
				.toList();

			totalDeleted += testGroups.size();
			groupRepository.deleteAll(testGroups);
			log.info("테스트 그룹 {} 개 삭제", testGroups.size());


			// 5. 테스트 사용자들 삭제 (kakaoId가 'test_kakao_'로 시작하는 것들)
			List<User> testUsers = userRepository.findAll()
				.stream()
				.filter(user -> user.getKakaoId().startsWith("test_kakao_"))
				.toList();

			totalDeleted += testUsers.size();
			userRepository.deleteAll(testUsers);

			totalDeleted += testUsers.size();
			userRepository.deleteAll(testUsers);
			long executionTime = System.currentTimeMillis() - startTime;
			log.info("=== 벌크 데이터 정리 완료 ===");

			return BulkDataResponse.cleanup(totalDeleted, executionTime);

		} catch (Exception e) {
			log.error("Bulk data cleanup failed", e);
			return BulkDataResponse.error("cleanup", e.getMessage());
		}
	}

}