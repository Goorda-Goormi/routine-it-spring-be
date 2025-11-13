package com.goormi.routine.domain.ranking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingUpdate {
	private Long userId;
	private Long groupId;
	private String monthYear;
	private Integer newScore;
	private ScoreCalculation scoreCalculation;
}
