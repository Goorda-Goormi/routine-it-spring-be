package com.goormi.routine.domain.group.dto.response;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class GroupResponse {

    private Long groupId;
    private String leaderName;
    private String groupName;
    private String description;
    private GroupType groupType;

    private LocalTime alarmTime;
    private String authDays;

    private String category;
    private String groupImageUrl;
    private int maxMembers;
    private int currentMemberCount;

    private boolean isAlarm;

    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GroupResponse from(Group group) {
        return GroupResponse.builder()
                .groupId(group.getGroupId())
                .leaderName(group.getLeader().getNickname())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .groupType(group.getGroupType())
                .alarmTime(group.getAlarmTime())
                .authDays(group.getAuthDays())
                .category(group.getCategory())
                .groupImageUrl(group.getGroupImageUrl())
                .maxMembers(group.getMaxMembers())
                .currentMemberCount(group.getCurrentMemberCnt())
                .isAlarm(group.getIsAlarm())
                .isActive(group.isActive())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
