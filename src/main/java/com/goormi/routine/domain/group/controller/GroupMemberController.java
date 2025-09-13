package com.goormi.routine.domain.group.controller;

import com.goormi.routine.domain.group.dto.request.GroupJoinRequest;
import com.goormi.routine.domain.group.dto.request.LeaderAnswerRequest;
import com.goormi.routine.domain.group.dto.response.GroupMemberResponse;
import com.goormi.routine.domain.group.entity.GroupMemberRole;
import com.goormi.routine.domain.group.entity.GroupMemberStatus;
import com.goormi.routine.domain.group.service.GroupMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/group/{groupId}")
@Tag(name = "그룹 멤버 API", description = "특정 그룹에서의 멤버 관리 API")
public class GroupMemberController {

    private final GroupMemberService groupMemberService;

    /**
     * 그룹에 멤버 가입 요청
     */
    @Operation(summary = "그룹 가입 요청 (인증 필요)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 가입 요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
    })
    @PostMapping("/join")
    public ResponseEntity<GroupMemberResponse> joinGroup(@AuthenticationPrincipal Long userId,
                                                                                      @PathVariable Long groupId,
                                                                                      @Valid @RequestBody GroupJoinRequest request) {

        GroupMemberResponse response = groupMemberService.addMember(userId, groupId, request);

        return ResponseEntity.ok(response);
    }
    /**
     *  리더에게 인증 요구
     */
    @Operation(summary = "리더에게 인증요청")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 요청 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
    })
    @PostMapping("/approve-auth")
    public ResponseEntity<Void> approveAuth(@AuthenticationPrincipal Long leaderId,
                                            @PathVariable Long groupId,
                                            @RequestBody LeaderAnswerRequest leaderAnswerRequest) {

        groupMemberService.approveAuthRequest(leaderId, groupId,leaderAnswerRequest);
        return ResponseEntity.ok().build();
    }

    /**
     *  내 그룹 멤버 정보 조회
     */
    @Operation(summary = "내 그룹 멤버 정보 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 그룹 멤버 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/members/me")
    public ResponseEntity<GroupMemberResponse> getGroupMemberMyInfo(@AuthenticationPrincipal Long userId,
                                                                    @PathVariable Long groupId) {
        GroupMemberResponse groupMemberInfo = groupMemberService.getGroupMemberInfo(groupId, userId);
        return ResponseEntity.ok(groupMemberInfo);
    }


    /**
     * 그룹의 멤버 목록 조회 (필터링 가능)
     */
    @Operation(summary = "그룹 멤버 조회")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 멤버 목록 조회 성공")
    })
    @GetMapping("/members")
    public ResponseEntity<List<GroupMemberResponse>> getGroupMembers(
            @PathVariable Long groupId,
            @RequestParam(required = false) GroupMemberRole role,
            @RequestParam(required = false) GroupMemberStatus status) {

        List<GroupMemberResponse> responses;
        if (role != null && status != null) {
            List<GroupMemberResponse> byRole = groupMemberService.getGroupMembersByRole(groupId, role);
            List<GroupMemberResponse> byStatus = groupMemberService.getGroupMembersByStatus(groupId, status);
            byRole.retainAll(byStatus);
            responses = byRole;
        } else if (role != null) {
            responses = groupMemberService.getGroupMembersByRole(groupId, role);
        } else if (status != null) {
            responses = groupMemberService.getGroupMembersByStatus(groupId, status);
        } else {
            // 기본적으로는 JOINED 상태의 멤버 목록을 반환
            responses = groupMemberService.getJoinedGroupMembersWithActivity(groupId);
        }
        return ResponseEntity.ok(responses);
    }

    /**
     * 그룹 리더가 멤버의 상태를 변경 (신청/가입/차단/떠남)
     */
    @PutMapping("/members/status")
    @Operation(summary = "그룹 멤버 상태 변경 (인증 필요)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 멤버 상태 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한이 없는 사용자")
    })
    public ResponseEntity<GroupMemberResponse> updateMemberStatus(@AuthenticationPrincipal Long leaderId,
                                                                               @Valid @RequestBody LeaderAnswerRequest request) {
        GroupMemberResponse response = groupMemberService.updateMemberStatus(leaderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 그룹 리더가 멤버의 역할을 변경 (리더 위임 등)
     */
    @PutMapping("/members/role")
    @Operation(summary = "그룹 멤버 역할 변경 (인증 필요)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 멤버 역할 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "권한이 없는 사용자")
    })
    public ResponseEntity<GroupMemberResponse> updateMemberRole(@AuthenticationPrincipal Long userId,
                                                                             @Valid @RequestBody LeaderAnswerRequest request) {
        GroupMemberResponse response = groupMemberService.updateMemberRole(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 알림 변경
     */
    @PutMapping("/members/me")
    @Operation(summary = "그룹멤버의 알림 변경 (인증 필요)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 멤버 알림 변경 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
    })
    public ResponseEntity<Void> updateIsAlarm(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long groupId,
                                              @RequestParam(required = true) boolean isAlarm) {

        groupMemberService.updateIsAlarm(groupId, userId, isAlarm);
        return ResponseEntity.ok().build();
    }

    /**
     * 그룹 탈퇴 (자발적 탈퇴)
     */
    @Operation(summary = "그룹 탈퇴 (인증 필요)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "그룹 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "그룹이나 유저를 찾을 수 없음")
    })
    @DeleteMapping("/members/me")
    public ResponseEntity<Void> leaveGroup(@AuthenticationPrincipal Long userId,
                                                                        @PathVariable Long groupId) {
        groupMemberService.delete(userId, groupId);
        return ResponseEntity.noContent().build();
    }
}
