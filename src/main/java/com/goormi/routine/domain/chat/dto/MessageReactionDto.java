package com.goormi.routine.domain.chat.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReactionDto {

    private Long id;
    private Long messageId;
    private Long userId;
    private String emoji;
    private LocalDateTime createdAt;
}