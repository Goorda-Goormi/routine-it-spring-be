package com.goormi.routine.domain.chat.service;

import com.goormi.routine.domain.chat.dto.ChatMessageDto;
import com.goormi.routine.domain.chat.dto.ChatRoomDto;
import com.goormi.routine.domain.chat.dto.CreateChatRoomRequest;
import com.goormi.routine.domain.chat.dto.ReactionSummaryDto;
import com.goormi.routine.domain.chat.entity.ChatMember;
import com.goormi.routine.domain.chat.entity.ChatMember.MemberRole;
import com.goormi.routine.domain.chat.entity.ChatMessage;
import com.goormi.routine.domain.chat.entity.ChatRoom;
import com.goormi.routine.domain.chat.repository.ChatMemberRepository;
import com.goormi.routine.domain.chat.repository.ChatMessageRepository;
import com.goormi.routine.domain.chat.repository.ChatRoomRepository;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MessageReactionService messageReactionService;
    
    @Override
    public ChatRoomDto createRoom(CreateChatRoomRequest request, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        GroupMember groupMember = groupMemberRepository.findByGroupIdAndUserIdAndIsActiveTrue(request.getGroupId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("그룹에 속해있지 않은 사용자입니다"));
        
        if (chatRoomRepository.existsByGroupIdAndRoomNameAndIsActiveTrue(request.getGroupId(), request.getRoomName())) {
            throw new IllegalArgumentException("이미 존재하는 채팅방 이름입니다");
        }
        
        ChatRoom chatRoom = ChatRoom.builder()
                .groupId(request.getGroupId())
                .roomName(request.getRoomName())
                .description(request.getDescription())
                .maxParticipants(request.getMaxParticipants())
                .isActive(true)
                .createdBy(user.getId())
                .build();
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        ChatMember creator = ChatMember.builder()
                .roomId(savedRoom.getId())
                .userId(user.getId())
                .role(MemberRole.ADMIN)
                .isActive(true)
                .build();
        
        chatMemberRepository.save(creator);
        
        return convertToDto(savedRoom, user.getNickname(), 1);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ChatRoomDto> getRooms(Long groupId, Pageable pageable) {
        Page<ChatRoom> rooms;

        if (groupId != null) {
            rooms = chatRoomRepository.findByGroupIdAndIsActiveTrue(groupId, pageable);
        } else {
            rooms = chatRoomRepository.findByIsActiveTrue(pageable);
        }

        // Batch로 데이터 조회하여 N+1 문제 해결
        List<Long> roomIds = rooms.getContent().stream()
                .map(ChatRoom::getId)
                .collect(Collectors.toList());

        List<Long> creatorIds = rooms.getContent().stream()
                .map(ChatRoom::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Long> participantCountMap = chatMemberRepository.countActiveMembersByRoomIds(roomIds)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));

        // 생성자 정보를 한 번에 조회
        Map<Long, String> creatorNicknameMap = userRepository.findAllById(creatorIds)
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return rooms.map(room -> {
            int participantCount = participantCountMap.getOrDefault(room.getId(), 0L).intValue();
            String creatorNickname = creatorNicknameMap.getOrDefault(room.getCreatedBy(), "Unknown");
            return convertToDto(room, creatorNickname, participantCount);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> getMyRooms(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        List<ChatRoom> rooms = chatRoomRepository.findActiveRoomsByUserId(user.getId());

        // Batch로 데이터 조회하여 N+1 문제 해결
        List<Long> roomIds = rooms.stream()
                .map(ChatRoom::getId)
                .collect(Collectors.toList());

        List<Long> creatorIds = rooms.stream()
                .map(ChatRoom::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        // 참가자 수를 한 번에 조회
        Map<Long, Long> participantCountMap = chatMemberRepository.countActiveMembersByRoomIds(roomIds)
                .stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));

        // 생성자 정보를 한 번에 조회
        Map<Long, String> creatorNicknameMap = userRepository.findAllById(creatorIds)
                .stream()
                .collect(Collectors.toMap(User::getId, User::getNickname));

        return rooms.stream()
                .map(room -> {
                    int participantCount = participantCountMap.getOrDefault(room.getId(), 0L).intValue();
                    String creatorNickname = creatorNicknameMap.getOrDefault(room.getCreatedBy(), "Unknown");
                    return convertToDto(room, creatorNickname, participantCount);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = "chatRoom", key = "#roomId")
    @Transactional(readOnly = true)
    public ChatRoomDto getRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.findByIdAndIsActiveTrue(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        int participantCount = chatMemberRepository.countActiveMembers(roomId);
        User creator = userRepository.findById(room.getCreatedBy()).orElse(null);
        String creatorNickname = creator != null ? creator.getNickname() : "Unknown";

        return convertToDto(room, creatorNickname, participantCount);
    }
    
    @Override
    @Cacheable(value = "chatRoomByGroup", key = "#groupId")
    @Transactional(readOnly = true)
    public ChatRoomDto getRoomByGroupId(Long groupId) {
        ChatRoom room = chatRoomRepository.findFirstByGroupIdAndIsActiveTrue(groupId)
                .orElseThrow(() -> new IllegalArgumentException("해당 그룹의 채팅방을 찾을 수 없습니다"));

        int participantCount = chatMemberRepository.countActiveMembers(room.getId());
        User creator = userRepository.findById(room.getCreatedBy()).orElse(null);
        String creatorNickname = creator != null ? creator.getNickname() : "Unknown";

        return convertToDto(room, creatorNickname, participantCount);
    }
    
    @Override
    @CacheEvict(value = "chatRoom", key = "#roomId")
    public void joinRoom(Long roomId, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        ChatRoom room = chatRoomRepository.findByIdAndIsActiveTrue(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다"));

        GroupMember groupMember = groupMemberRepository.findByGroupIdAndUserIdAndIsActiveTrue(room.getGroupId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("그룹에 속해있지 않은 사용자입니다"));

        chatMemberRepository.findByRoomIdAndUserId(roomId, user.getId()).ifPresentOrElse(
            member -> {
                if (!member.getIsActive()) {
                    member.setIsActive(true);
                    member.setJoinedAt(LocalDateTime.now());
                    member.setLeftAt(null);
                    chatMemberRepository.save(member);
                } else {
                    throw new IllegalArgumentException("이미 참여중인 채팅방입니다");
                }
            },
            () -> {
                int currentCount = chatMemberRepository.countActiveMembers(roomId);
                if (room.getMaxParticipants() != null && currentCount >= room.getMaxParticipants()) {
                    throw new IllegalArgumentException("채팅방 인원이 가득 찼습니다");
                }

                ChatMember newMember = ChatMember.builder()
                        .roomId(roomId)
                        .userId(user.getId())
                        .role(MemberRole.MEMBER)
                        .isActive(true)
                        .build();

                chatMemberRepository.save(newMember);
            }
        );
    }
    
    @Override
    @CacheEvict(value = "chatRoom", key = "#roomId")
    public void leaveRoom(Long roomId, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        ChatMember member = chatMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(roomId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다"));

        member.setIsActive(false);
        member.setLeftAt(LocalDateTime.now());
        chatMemberRepository.save(member);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ChatMessageDto> getMessages(Long roomId, Long beforeMessageId, Pageable pageable, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        if (!chatMemberRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, user.getId())) {
            throw new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다");
        }

        Page<ChatMessage> messages;
        if (beforeMessageId != null) {
            messages = chatMessageRepository.findByRoomIdAndIdLessThanOrderByCreatedAtDesc(roomId, beforeMessageId, pageable);
        } else {
            messages = chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        }

        // Load reactions for all messages in the page
        List<Long> messageIds = messages.getContent().stream()
                .map(ChatMessage::getId)
                .collect(Collectors.toList());

        Map<Long, List<ReactionSummaryDto>> reactionsMap = messageReactionService.getReactionsByMessageIds(messageIds);

        return messages.map(message -> convertMessageToDto(message, reactionsMap.get(message.getId())));
    }
    
    private ChatRoomDto convertToDto(ChatRoom room, String creatorNickname, int participantCount) {
        return ChatRoomDto.builder()
                .id(room.getId())
                .groupId(room.getGroupId())
                .roomName(room.getRoomName())
                .description(room.getDescription())
                .maxParticipants(room.getMaxParticipants())
                .currentParticipants(participantCount)
                .isActive(room.getIsActive())
                .createdBy(room.getCreatedBy())
                .creatorNickname(creatorNickname)
                .createdAt(room.getCreatedAt())
                .build();
    }
    
    private ChatMessageDto convertMessageToDto(ChatMessage message) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .userId(message.getUserId())
                .senderNickname(message.getSenderNickname())
                .message(message.getMessage())
                .imageUrl(message.getImageUrl())
                .messageType(message.getMessageType())
                .sentAt(message.getCreatedAt())
                .isApproved(message.getIsApproved() != null && message.getIsApproved())
                .build();
    }

    private ChatMessageDto convertMessageToDto(ChatMessage message, List<ReactionSummaryDto> reactions) {
        return ChatMessageDto.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .userId(message.getUserId())
                .senderNickname(message.getSenderNickname())
                .message(message.getMessage())
                .imageUrl(message.getImageUrl())
                .messageType(message.getMessageType())
                .sentAt(message.getCreatedAt())
                .isApproved(message.getIsApproved() != null && message.getIsApproved())
                .reactions(reactions)
                .build();
    }
}