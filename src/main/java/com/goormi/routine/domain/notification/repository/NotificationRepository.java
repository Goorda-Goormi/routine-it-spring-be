package com.goormi.routine.domain.notification.repository;

import com.goormi.routine.domain.notification.entity.Notification;
import com.goormi.routine.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.sender " +
            "LEFT JOIN FETCH n.group " +
            "WHERE n.receiver.id = :receiverId ORDER BY n.createdAt DESC")
    List<Notification> findByReceiver_IdOrderByCreatedAtDesc(@Param("receiverId") Long receiverId);
    @Query("SELECT n FROM Notification n " +
            "JOIN FETCH n.sender " +
            "LEFT JOIN FETCH n.group " +
            "WHERE n.receiver.id = :receiverId AND n.notificationType = :type ORDER BY n.createdAt DESC")
    List<Notification> findByReceiver_IdAndNotificationType(@Param("receiverId") Long receiverId, @Param("type") NotificationType type);
}
