package com.goormi.routine.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "sender_nickname", nullable = false, length = 50)
    private String senderNickname;
    
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Boolean isApproved;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isAuthMessage() {
        return MessageType.NOTICE == this.messageType;
    }

    public void approveMessage() {
        this.isApproved = true;
    }
    public void rejectMessage() {
        this.isApproved = false;
    }
    
    public enum MessageType {
        TALK,           // 일반 대화
        NOTICE,         // 시스템 공지
        MEMBER_JOIN,    // 그룹 멤버 가입 (영구적, DB 저장)
        MEMBER_LEAVE,   // 그룹 멤버 탈퇴 (영구적, DB 저장)
        ONLINE,         // 온라인 상태 (임시적, DB 저장 안함)
        OFFLINE,        // 오프라인 상태 (임시적, DB 저장 안함)
        
        // 하위 호환성을 위해 유지 (deprecated)
        @Deprecated
        ENTER,
        @Deprecated  
        LEAVE
    }
}