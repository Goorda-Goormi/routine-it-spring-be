package com.goormi.routine.domain.notification.controller;

import com.goormi.routine.domain.notification.dto.NotificationResponse;
import com.goormi.routine.domain.notification.entity.NotificationType;
import com.goormi.routine.domain.notification.service.NotificationService;
import com.goormi.routine.domain.notification.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
@Tag(name = "notification API", description = "그룹 가입, 그룹멤터 상태 역할 변경 알림 API")
public class NotificationController {
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "알림 구독 (SSE)", description = "SSE를 통해 실시간으로 알림을 구독합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "구독 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<SseEmitter> subscribe(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEmitterId) {
        return ResponseEntity.ok(sseEmitterService.subscribe(userId, lastEmitterId));
    }

    @GetMapping()
    @Operation(summary = "유저의 알림 전체 조회", description = "인증된 사용자의 전체 알림을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal Long receiverId) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByReceiver(receiverId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/type")
    @Operation(summary = "유저의 알림타입 별 조회", description = "인증된 사용자의 알림을 타입별로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<List<NotificationResponse>> getNotificationsByType(
            @AuthenticationPrincipal Long receiverId,
            @RequestParam NotificationType notificationType) {
        List<NotificationResponse> notifications = notificationService.getNotificationsByNotificationType(receiverId, notificationType);
        return ResponseEntity.ok(notifications);
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "유저의 알림 읽음 표시", description = "인증된 사용자가 알림을 읽었습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<NotificationResponse> readNotification(
            @AuthenticationPrincipal Long userId,
            @PathVariable("notificationId") Long notificationId,
            @RequestParam boolean isRead) {

        notificationService.updateIsRead(notificationId, userId, isRead);
        return ResponseEntity.ok().build();
    }

}
