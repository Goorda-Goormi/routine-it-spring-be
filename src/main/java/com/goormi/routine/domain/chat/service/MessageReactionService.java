package com.goormi.routine.domain.chat.service;

import com.goormi.routine.domain.chat.dto.MessageReactionDto;
import com.goormi.routine.domain.chat.dto.ReactionSummaryDto;

import java.util.List;
import java.util.Map;

public interface MessageReactionService {

    MessageReactionDto addReaction(Long messageId, Long userId, String emoji);

    void removeReaction(Long messageId, Long userId, String emoji);

    List<MessageReactionDto> getReactionsByMessageId(Long messageId);

    Map<Long, List<ReactionSummaryDto>> getReactionsByMessageIds(List<Long> messageIds);

    List<ReactionSummaryDto> getReactionSummary(Long messageId);
}