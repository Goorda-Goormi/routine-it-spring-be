package com.goormi.routine.domain.notification.service;

import com.goormi.routine.domain.notification.dto.NotificationResponse;
import com.goormi.routine.domain.user.entity.User;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseEmitterService {
    SseEmitter subscribe(Long userId, String lastEmitterId);
    void sendToClient(Long userId, Object data);
    void sendNotification(User receiver, NotificationResponse notificationResponse);
}
