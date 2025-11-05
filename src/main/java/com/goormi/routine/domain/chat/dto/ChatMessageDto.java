package com.goormi.routine.domain.chat.dto;

import com.goormi.routine.domain.chat.entity.ChatMessage.MessageType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private Long id;
    private Long roomId;
    private Long userId;
    private String senderNickname;
    private String message;
    private String imageUrl;
    private MessageType messageType;
    private LocalDateTime sentAt;
    private Boolean isApproved;
    private List<ReactionSummaryDto> reactions;
}