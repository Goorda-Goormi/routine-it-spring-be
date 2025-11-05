package com.goormi.routine.domain.chat.service;

import com.goormi.routine.domain.chat.dto.ChatMessageDto;
import com.goormi.routine.domain.chat.entity.ChatMember;
import com.goormi.routine.domain.chat.entity.ChatMessage;
import com.goormi.routine.domain.chat.entity.ChatMessage.MessageType;
import com.goormi.routine.domain.chat.entity.ChatRoom;
import com.goormi.routine.domain.chat.repository.ChatMemberRepository;
import com.goormi.routine.domain.chat.repository.ChatMessageRepository;
import com.goormi.routine.domain.chat.repository.ChatRoomRepository;
import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.notification.entity.NotificationType;
import com.goormi.routine.domain.notification.service.NotificationService;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {
    
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final RedisMessagePublisher redisMessagePublisher;

    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public ChatMessageDto saveAndSendMessage(ChatMessageDto messageDto, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        ChatMember member = chatMemberRepository.findByRoomIdAndUserIdAndIsActiveTrue(messageDto.getRoomId(), user.getId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방에 참여하지 않은 사용자입니다"));

        if (messageDto.getMessageType() != MessageType.TALK &&  messageDto.getMessageType() != MessageType.NOTICE) {
            throw new IllegalArgumentException("Invalid message type");
        }
        ChatMessage message = ChatMessage.builder()
                .roomId(messageDto.getRoomId())
                .userId(user.getId())
                .senderNickname(user.getNickname())
                .message(messageDto.getMessage())
                .messageType(messageDto.getMessageType())
                .imageUrl(messageDto.getImageUrl())
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(message);

        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("chatRoom not found"));
        Group group = groupRepository.findById(chatRoom.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("group not found"));

        if (savedMessage.isAuthMessage()){
            notificationService.createNotification(
                    NotificationType.GROUP_TODAY_AUTH_REQUEST, userId, group.getLeader().getId(), group.getGroupId());
            savedMessage.rejectMessage();
        }
        chatMemberRepository.updateLastReadMessage(messageDto.getRoomId(), user.getId(), savedMessage.getId());
        
        ChatMessageDto dto = convertToDto(savedMessage);
        redisMessagePublisher.publish(dto);
        
        return dto;
    }
    
    @Override
    public ChatMessageDto handleUserEnter(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .userId(user.getId())
                .senderNickname(user.getNickname())
                .message(user.getNickname() + "님이 입장하셨습니다.")
                .messageType(MessageType.ENTER)
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        ChatMessageDto dto = convertToDto(savedMessage);
        redisMessagePublisher.publish(dto);
        
        return dto;
    }
    
    @Override
    public ChatMessageDto handleUserLeave(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .userId(user.getId())
                .senderNickname(user.getNickname())
                .message(user.getNickname() + "님이 퇴장하셨습니다.")
                .messageType(MessageType.LEAVE)
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        
        ChatMessageDto dto = convertToDto(savedMessage);
        redisMessagePublisher.publish(dto);
        
        return dto;
    }
    
    @Override
    public ChatMessageDto notifyMemberJoin(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .userId(user.getId())
                .senderNickname(user.getNickname())
                .message(user.getNickname() + "님이 그룹에 참여했습니다.")
                .messageType(MessageType.MEMBER_JOIN)
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto dto = convertToDto(savedMessage);
        redisMessagePublisher.publish(dto);
        
        log.info("Member join notification sent: {} in room {}", user.getNickname(), roomId);
        return dto;
    }
    
    @Override
    public ChatMessageDto notifyMemberLeave(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        ChatMessage message = ChatMessage.builder()
                .roomId(roomId)
                .userId(user.getId())
                .senderNickname(user.getNickname())
                .message(user.getNickname() + "님이 그룹을 나갔습니다.")
                .messageType(MessageType.MEMBER_LEAVE)
                .build();
        
        ChatMessage savedMessage = chatMessageRepository.save(message);
        ChatMessageDto dto = convertToDto(savedMessage);
        redisMessagePublisher.publish(dto);
        
        log.info("Member leave notification sent: {} in room {}", user.getNickname(), roomId);
        return dto;
    }
    
    @Override
    public void handleUserOnline(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // Redis에 온라인 상태 저장 (DB 저장 안함)
        redisTemplate.opsForSet().add("room:" + roomId + ":online", userId.toString());
        
        // 실시간으로만 전송 (DB 저장 안함)
        ChatMessageDto dto = ChatMessageDto.builder()
                .roomId(roomId)
                .userId(userId)
                .senderNickname(user.getNickname())
                .messageType(MessageType.ONLINE)
                .build();
        
        redisMessagePublisher.publish(dto);
        log.debug("User {} is now online in room {}", user.getNickname(), roomId);
    }
    
    @Override
    public void handleUserOffline(Long roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        
        // Redis에서 온라인 상태 제거
        redisTemplate.opsForSet().remove("room:" + roomId + ":online", userId.toString());
        
        // 실시간으로만 전송 (DB 저장 안함)
        ChatMessageDto dto = ChatMessageDto.builder()
                .roomId(roomId)
                .userId(userId)
                .senderNickname(user.getNickname())
                .messageType(MessageType.OFFLINE)
                .build();
        
        redisMessagePublisher.publish(dto);
        log.debug("User {} is now offline in room {}", user.getNickname(), roomId);
    }
    
    private ChatMessageDto convertToDto(ChatMessage message) {
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
}