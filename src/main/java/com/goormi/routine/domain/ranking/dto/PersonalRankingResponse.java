package com.goormi.routine.domain.ranking.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalRankingResponse {
	private Long userId;
	private String nickname;
	private String profileImageUrl;
	private Integer totalScore;
	private Integer currentRank;
	private Integer totalParticipants;
	private String monthYear;
	private Integer consecutiveDays;
	private List<GroupRankingDetail> groupDetails;
	private Boolean isCurrentUser;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updatedAt;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class GroupRankingDetail {
		private Long groupId;
		private String groupName;
		private String groupImageUrl;
		private String groupType; // 자유 참여 or 의무 참여
		private Integer score;
		private Integer rankInGroup;
		private Integer totalMembers;
		private Integer authCount;
		private Double groupWeightMultiplier; // 자유 참여(1.2) or 의무 참여(1.5)
	}
}