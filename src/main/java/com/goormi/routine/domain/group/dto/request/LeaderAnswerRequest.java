package com.goormi.routine.domain.group.dto.request;

import com.goormi.routine.domain.group.entity.GroupMemberRole;
import com.goormi.routine.domain.group.entity.GroupMemberStatus;
import com.goormi.routine.domain.userActivity.entity.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class LeaderAnswerRequest {
    private Long groupId;
    private Long leaderId;
    private Long targetMemberId;
    private Long chatMsgId;

    @Schema(description = "업데이트 할 그룹 멤버 상태")
    private GroupMemberStatus status;
    @Schema(description = "업데이트 할 그룹 멤버 역할")
    private GroupMemberRole role;

    // 유저액티비티 request
    @Schema(description = "인증 요청 수락 여부")
    private Boolean isApproved;
    @Schema(description = "인증 요구하는 활동 날짜")
    private LocalDate activityDate;
    @Schema(description = "활동 인증 이미지 URL")
    private String imageUrl;
}
