package com.goormi.routine.domain.chat.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionSummaryDto {

    private String emoji;
    private Integer count;
    private List<Long> userIds;
}