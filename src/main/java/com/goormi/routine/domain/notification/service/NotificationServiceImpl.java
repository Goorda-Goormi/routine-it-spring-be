package com.goormi.routine.domain.notification.service;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.notification.dto.NotificationResponse;
import com.goormi.routine.domain.notification.entity.Notification;
import com.goormi.routine.domain.notification.entity.NotificationType;
import com.goormi.routine.domain.notification.repository.NotificationRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    public NotificationResponse createNotification(NotificationType notificationType,
                                                   Long senderId, Long receiverId, Long groupId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(()-> new IllegalArgumentException("Group not found"));

        GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, receiver)
                .orElseThrow(() -> new IllegalArgumentException("GroupMember not found"));

        String content = "";
        if (notificationType == NotificationType.GROUP_JOIN_REQUEST) {
            content = sender.getNickname() + "님이 "
                    + group.getGroupName() +"에 그룹 가입 요청을 보냈습니다.";
        } else if (notificationType == NotificationType.GROUP_MEMBER_ROLE_UPDATED){
            content = receiver.getNickname() + "님의 "
                    + group.getGroupName() +"의 멤버 역할이 "
                    + groupMember.getStatus()+"으로 변경되었습니다.";
        } else if (notificationType == NotificationType.GROUP_MEMBER_STATUS_UPDATED) {
            content = receiver.getNickname() + "님의 "
                    + group.getGroupName() +"의 멤버 상태가 "
                    + groupMember.getRole()+"으로 변경되었습니다.";
        } else if (notificationType == NotificationType.GROUP_TODAY_AUTH_COMPLETED) {
            content = sender.getNickname() + "님이 " + receiver.getNickname() + "님의 "
                    + group.getGroupName() +"의 그룹 인증을 수락했습니다.";
        } else if (notificationType == NotificationType.GROUP_TODAY_AUTH_REJECTED) {
            content = sender.getNickname() + "님이 " + receiver.getNickname() + "님의 "
                    + group.getGroupName() +"의 그룹 인증을 반려했습니다.";
        } else if (notificationType == NotificationType.GROUP_TODAY_AUTH_REQUEST) {
            content = sender.getNickname() + "님이 "
                    + group.getGroupName() +"의 그룹 인증을 요청했습니다.";
        } else if (notificationType == NotificationType.MONTHLY_REVIEW) {
            String monthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            content = receiver.getNickname() + "님의 "
                + monthYear + " 월간 루틴 성과 리포트가 준비되었습니다! 확인해보세요.";
        }
      
        Notification notification =
                Notification.createNotification(content, notificationType, sender, receiver, group);
      
        Notification saved = notificationRepository.save(notification);

        return NotificationResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByReceiver(Long receiverId) {
        List<Notification> notifications = notificationRepository.findByReceiver_IdOrderByCreatedAtDesc(receiverId);
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByNotificationType(Long receiverId, NotificationType notificationType) {
        List<Notification> notifications = notificationRepository.findByReceiver_IdAndNotificationType(receiverId, notificationType);
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Override
    public void updateIsRead(Long notificationId, Long receiverId, Boolean isRead) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!Objects.equals(notification.getReceiver().getId(), receiver.getId())) {
            throw new IllegalArgumentException("user id not equals to receiver id");
        }

        notification.updateIsRead(isRead);
    }
}
