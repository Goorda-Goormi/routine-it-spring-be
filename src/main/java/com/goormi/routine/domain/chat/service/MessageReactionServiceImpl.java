package com.goormi.routine.domain.chat.service;

import com.goormi.routine.domain.chat.dto.MessageReactionDto;
import com.goormi.routine.domain.chat.dto.ReactionSummaryDto;
import com.goormi.routine.domain.chat.entity.MessageReaction;
import com.goormi.routine.domain.chat.repository.ChatMemberRepository;
import com.goormi.routine.domain.chat.repository.ChatMessageRepository;
import com.goormi.routine.domain.chat.repository.MessageReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageReactionServiceImpl implements MessageReactionService {

    private final MessageReactionRepository messageReactionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberRepository chatMemberRepository;

    @Override
    public MessageReactionDto addReaction(Long messageId, Long userId, String emoji) {
        if (!chatMessageRepository.existsById(messageId)) {
            throw new IllegalArgumentException("메시지를 찾을 수 없습니다");
        }

        if (messageReactionRepository.existsByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)) {
            throw new IllegalArgumentException("이미 동일한 이모지를 추가했습니다");
        }

        MessageReaction reaction = MessageReaction.builder()
                .messageId(messageId)
                .userId(userId)
                .emoji(emoji)
                .build();

        MessageReaction savedReaction = messageReactionRepository.save(reaction);
        log.info("Reaction added: userId={}, messageId={}, emoji={}", userId, messageId, emoji);

        return convertToDto(savedReaction);
    }

    @Override
    public void removeReaction(Long messageId, Long userId, String emoji) {
        MessageReaction reaction = messageReactionRepository.findByMessageIdAndUserIdAndEmoji(messageId, userId, emoji)
                .orElseThrow(() -> new IllegalArgumentException("해당 리액션을 찾을 수 없습니다"));

        messageReactionRepository.delete(reaction);
        log.info("Reaction removed: userId={}, messageId={}, emoji={}", userId, messageId, emoji);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageReactionDto> getReactionsByMessageId(Long messageId) {
        List<MessageReaction> reactions = messageReactionRepository.findByMessageId(messageId);
        return reactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<ReactionSummaryDto>> getReactionsByMessageIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return new HashMap<>();
        }

        List<MessageReaction> reactions = messageReactionRepository.findByMessageIdIn(messageIds);

        Map<Long, Map<String, List<Long>>> groupedReactions = reactions.stream()
                .collect(Collectors.groupingBy(
                        MessageReaction::getMessageId,
                        Collectors.groupingBy(
                                MessageReaction::getEmoji,
                                Collectors.mapping(MessageReaction::getUserId, Collectors.toList())
                        )
                ));

        Map<Long, List<ReactionSummaryDto>> result = new HashMap<>();
        for (Long messageId : messageIds) {
            Map<String, List<Long>> emojiMap = groupedReactions.getOrDefault(messageId, new HashMap<>());
            List<ReactionSummaryDto> summaries = emojiMap.entrySet().stream()
                    .map(entry -> ReactionSummaryDto.builder()
                            .emoji(entry.getKey())
                            .count(entry.getValue().size())
                            .userIds(entry.getValue())
                            .build())
                    .collect(Collectors.toList());
            result.put(messageId, summaries);
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReactionSummaryDto> getReactionSummary(Long messageId) {
        List<MessageReaction> reactions = messageReactionRepository.findByMessageId(messageId);

        Map<String, List<Long>> groupedByEmoji = reactions.stream()
                .collect(Collectors.groupingBy(
                        MessageReaction::getEmoji,
                        Collectors.mapping(MessageReaction::getUserId, Collectors.toList())
                ));

        return groupedByEmoji.entrySet().stream()
                .map(entry -> ReactionSummaryDto.builder()
                        .emoji(entry.getKey())
                        .count(entry.getValue().size())
                        .userIds(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private MessageReactionDto convertToDto(MessageReaction reaction) {
        return MessageReactionDto.builder()
                .id(reaction.getId())
                .messageId(reaction.getMessageId())
                .userId(reaction.getUserId())
                .emoji(reaction.getEmoji())
                .createdAt(reaction.getCreatedAt())
                .build();
    }
}