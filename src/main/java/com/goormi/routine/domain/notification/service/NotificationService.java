package com.goormi.routine.domain.notification.service;

import com.goormi.routine.domain.notification.dto.NotificationResponse;
import com.goormi.routine.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    NotificationResponse createNotification(NotificationType notificationType,
                                            Long senderId, Long receiverId, Long groupId);

    Page<NotificationResponse> getNotificationsByReceiver(Long receiverId, Pageable pageable);

    Page<NotificationResponse> getNotificationsByNotificationType(Long receiverId, NotificationType notificationType, Pageable pageable);

    void updateIsRead(Long notificationId, Long receiverId, boolean isRead);
}
