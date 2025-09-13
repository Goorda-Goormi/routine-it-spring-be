package com.goormi.routine.domain.group.dto.request;

import com.goormi.routine.domain.group.entity.GroupType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class GroupCreateRequest {

    @NotBlank(message = "그룹 이름은 필수입니다.")
    private String groupName;
    private String groupDescription;
    @NotNull(message = "그룹 타입을 지정해주세요")
    private GroupType groupType;

    private LocalTime alarmTime;
    @Pattern(regexp = "^[01]{7}$", message = "인증 요일은 7자리 0,1로 구성되어야 합니다")
    private String authDays;

    private String category;
    private String imageUrl;
    private int maxMembers;

    private boolean isAlarm;

}
