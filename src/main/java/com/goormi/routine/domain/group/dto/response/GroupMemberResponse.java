package com.goormi.routine.domain.group.dto.response;

import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.entity.GroupMemberRole;
import com.goormi.routine.domain.group.entity.GroupMemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupMemberResponse {

    private Long userId;
    private Long groupMemberId;
    private String groupName;
    private String memberName;
    private GroupMemberStatus status;
    private GroupMemberRole role;
    @Schema(description = "메세지는 인증, 미인증 둘 중 하나를 반환합니다.")
    private String message;
    private boolean isAlarm;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static  GroupMemberResponse from(GroupMember groupMember) {
        return GroupMemberResponse.builder()
                .userId(groupMember.getUser().getId())
                .groupMemberId(groupMember.getMemberId())
                .groupName(groupMember.getGroup().getGroupName())
                .memberName(groupMember.getUser().getNickname())
                .status(groupMember.getStatus())
                .role(groupMember.getRole())
                .isAlarm(groupMember.isAlarm())
                .createdAt(groupMember.getCreatedAt())
                .updatedAt(groupMember.getUpdatedAt())
                .build();
    }
    public static  GroupMemberResponse from(GroupMember groupMember,Boolean isAuthToday) {
        return GroupMemberResponse.builder()
                .userId(groupMember.getUser().getId())
                .groupMemberId(groupMember.getMemberId())
                .groupName(groupMember.getGroup().getGroupName())
                .memberName(groupMember.getUser().getNickname())
                .status(groupMember.getStatus())
                .role(groupMember.getRole())
                .message(isAuthToday == true ? "인증" : "미인증")
                .isAlarm(groupMember.isAlarm())
                .createdAt(groupMember.getCreatedAt())
                .updatedAt(groupMember.getUpdatedAt())
                .build();
    }
}
