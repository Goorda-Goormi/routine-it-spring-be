package com.goormi.routine.domain.chat.controller;

import com.goormi.routine.common.response.ApiResponse;
import com.goormi.routine.domain.chat.dto.AddReactionRequest;
import com.goormi.routine.domain.chat.dto.MessageReactionDto;
import com.goormi.routine.domain.chat.dto.ReactionSummaryDto;
import com.goormi.routine.domain.chat.service.MessageReactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Message Reaction", description = "채팅 메시지 이모지 리액션 API")
@RestController
@RequestMapping("/api/chat/messages")
@RequiredArgsConstructor
public class MessageReactionController {

    private final MessageReactionService messageReactionService;

    @Operation(summary = "이모지 리액션 추가", description = "채팅 메시지에 이모지 리액션을 추가합니다")
    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<ApiResponse<MessageReactionDto>> addReaction(
            @PathVariable Long messageId,
            @Valid @RequestBody AddReactionRequest request,
            @AuthenticationPrincipal Long userId) {

        MessageReactionDto reaction = messageReactionService.addReaction(messageId, userId, request.getEmoji());
        return ResponseEntity.ok(ApiResponse.success(reaction));
    }

    @Operation(summary = "이모지 리액션 제거", description = "채팅 메시지에서 이모지 리액션을 제거합니다")
    @DeleteMapping("/{messageId}/reactions/{emoji}")
    public ResponseEntity<ApiResponse<Void>> removeReaction(
            @PathVariable Long messageId,
            @PathVariable String emoji,
            @AuthenticationPrincipal Long userId) {

        messageReactionService.removeReaction(messageId, userId, emoji);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "메시지의 모든 리액션 조회", description = "특정 메시지의 모든 이모지 리액션을 조회합니다")
    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<ApiResponse<List<MessageReactionDto>>> getReactions(
            @PathVariable Long messageId) {

        List<MessageReactionDto> reactions = messageReactionService.getReactionsByMessageId(messageId);
        return ResponseEntity.ok(ApiResponse.success(reactions));
    }

    @Operation(summary = "메시지의 리액션 요약 조회", description = "특정 메시지의 이모지별 리액션 요약 정보를 조회합니다")
    @GetMapping("/{messageId}/reactions/summary")
    public ResponseEntity<ApiResponse<List<ReactionSummaryDto>>> getReactionSummary(
            @PathVariable Long messageId) {

        List<ReactionSummaryDto> summary = messageReactionService.getReactionSummary(messageId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}