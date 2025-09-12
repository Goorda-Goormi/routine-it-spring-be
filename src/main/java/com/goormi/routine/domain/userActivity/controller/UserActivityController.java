package com.goormi.routine.domain.userActivity.controller;

import com.goormi.routine.domain.userActivity.dto.UserActivityRequest;
import com.goormi.routine.domain.userActivity.dto.UserActivityResponse;
import com.goormi.routine.domain.userActivity.service.UserActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user-activities")
@Tag(name = "UserActivity API", description = "사용자 활동 CRUD API")
public class UserActivityController {

    private final UserActivityService userActivityService;

    @Operation(summary = "특정 날짜의 사용자 활동 목록 조회", description = "인증된 사용자의 특정 날짜 활동 내역을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/day")
    public ResponseEntity<List<UserActivityResponse>> getUserActivities(
            @AuthenticationPrincipal Long userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<UserActivityResponse> activities = userActivityService.getUserActivitiesPerDay(userId, date);
        return ResponseEntity.ok(activities);
    }

    @Operation(summary = "유저의 인증 활동(사진) 조회", description = "타유저는 isPublic이 true인 경우만 조회가능")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사진 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping("/info")
    public ResponseEntity<List<UserActivityResponse>> getUserImages(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long targetUserId) {

        Long id = targetUserId != null ? targetUserId : userId;
        List<UserActivityResponse> activities = userActivityService.getImagesOfUserActivities(userId, id);
        return ResponseEntity.ok(activities);
    }



    @Operation(summary = "새로운 사용자 활동 생성", description = "인증된 사용자의 활동을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "활동 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/create")
    public ResponseEntity<UserActivityResponse> createActivity(@AuthenticationPrincipal Long userId,
                                                               @RequestBody @Valid UserActivityRequest request) {
        UserActivityResponse response = userActivityService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "사용자 활동 수정", description = "특정 활동의 타입을 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "활동 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "활동을 찾을 수 없음")
    })
    @PutMapping("/update")
    public ResponseEntity<UserActivityResponse> updateActivity(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserActivityRequest request) {
        UserActivityResponse response = userActivityService.updateActivity(userId, request);
        return ResponseEntity.ok(response);
    }


    //******** attendance ********//
    @Operation(summary = "특정 날짜 출석 여부", description = "개인루틴/그룹루틴 완료가 하나라도 있으면 해당 일자는 출석으로 인정합니다.")
    @GetMapping("/attendance/check")
    public ResponseEntity<Boolean> checkAttendance(
            @AuthenticationPrincipal Long userId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(userActivityService.hasAttendanceOn(userId, date));
    }

    @Operation(
            summary = "누적 출석 일수",
            description = "startDate~endDate 기간 동안 출석으로 인정된 서로 다른 '일자'의 개수를 반환합니다. " +
                    "파라미터를 생략하면 전체 기간(1970-01-01 ~ 오늘[KST])로 계산합니다." + " 타유저 id 값이 없으면 본인 누적출석일 조회, 있으면 타유저 누적 출석일 조회"
    )
    @GetMapping("/attendance/total")
    public ResponseEntity<Integer> getTotalAttendanceDays(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long targetUserId,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long id = targetUserId != null ? targetUserId : userId;
        int total = userActivityService.getTotalAttendanceDays(id, startDate, endDate);
        return ResponseEntity.ok(total);
    }
}