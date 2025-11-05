package com.goormi.routine.domain.calendar.service;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.entity.GroupMemberStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.List;

/**
 * 캘린더와 다른 도메인 간의 연동을 담당하는 이벤트 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
public class CalendarIntegrationService {

    private final CalendarService calendarService;
    private final CalendarMemberService calendarMemberService;

    @PostConstruct
    public void postConstruct() {
        log.info("CalendarIntegrationService initialized successfully.");
    }

    /**
     * 그룹 멤버 상태 변경 시 캘린더 일정 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupMemberStatusChange(GroupMemberStatusChangeEvent event) {
        GroupMember groupMember = event.getGroupMember();
        log.info("그룹 멤버 상태 변경 이벤트 수신: userId={}, groupId={}, newStatus={}",
                groupMember.getUser().getId(),
                groupMember.getGroup().getGroupId(),
                groupMember.getStatus());

        if (groupMember.getStatus() == GroupMemberStatus.JOINED) {
            // 그룹 가입 시 일정 생성
            calendarMemberService.handleMemberJoined(groupMember);
        } else {
            // 그룹 탈퇴 시 일정 삭제
            calendarMemberService.handleMemberLeft(groupMember);
        }
        log.info("그룹 멤버 상태 변경 이벤트 처리 완료: userId={}, groupId={}",
                groupMember.getUser().getId(), groupMember.getGroup().getGroupId());
    }
    /**
     * 그룹 멤버 알람 변경 시 캘린더 일정 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupMemberAlarmChange(GroupMemberAlarmChangeEvent event) {
        GroupMember groupMember = event.getGroupMember();
        Long userId = groupMember.getUser().getId();
        Group group = groupMember.getGroup();

        if (!calendarService.isCalendarConnected(userId)) {
            log.warn("캘린더 연동 안됨");
            return;
        }

        try {
            String eventId = groupMember.getCalendarEventId();
            calendarService.updateGroupSchedule(userId, group, eventId);
            log.info("그룹 멤버 알람 설정 변경으로 캘린더 일정 업데이트 완료: userId={}, eventId={}", userId, eventId);
        } catch (Exception e) {
            log.error("그룹 멤버 알람 설정 변경 업데이트 실패: groupMemberId={}, userId={}", groupMember.getMemberId(), userId, e);
        }
    }

    /**
     * 그룹 정보 변경 시 일정 업데이트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupInfoUpdate(GroupInfoUpdateEvent event) {
        Group group = event.getGroup();
        log.info("그룹 정보 변경 이벤트 수신: groupId={}, groupName={}",
                group.getGroupId(), group.getGroupName());

        List<GroupMember> membersWithCalendarEvent = group.getGroupMembers().stream()
                .filter(member -> member.getStatus() == GroupMemberStatus.JOINED && member.hasCalendarEvent())
                .toList();

        log.debug("그룹 정보 변경으로 인해 총 {}명의 멤버의 캘린더 일정을 업데이트합니다.", membersWithCalendarEvent.size());

        membersWithCalendarEvent.forEach(member -> {
            try {
                Long userId = member.getUser().getId();
                String eventId = member.getCalendarEventId();

                if (calendarService.isCalendarConnected(userId)) {
                    log.debug("캘린더 일정 업데이트 시도: userId={}, eventId={}", userId, eventId);
                    calendarService.updateGroupSchedule(userId, group, eventId);
                } else {
                    log.warn("캘린더 연동 안됨 - 일정 업데이트 스킵: userId={}", userId);
                }
            } catch (Exception e) {
                log.error("그룹 일정 업데이트 실패: groupMemberId={}, userId={}",
                        member.getMemberId(), member.getUser().getId(), e);
            }
        });

        log.info("그룹 정보 변경 이벤트 처리 완료: 총 {}명의 멤버 처리", membersWithCalendarEvent.size());
    }

    /**
     * 그룹 삭제 시 모든 관련 일정 삭제
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupDeletion(GroupDeletionEvent event) {
        Group group = event.getGroup();
        log.info("그룹 삭제 이벤트 수신: groupId={}, groupName={}",
                group.getGroupId(), group.getGroupName());

        List<GroupMember> membersWithCalendarEvent = group.getGroupMembers().stream()
                .filter(member -> member.getStatus() == GroupMemberStatus.JOINED && member.hasCalendarEvent())
                .toList();

        log.debug("그룹 삭제로 인해 총 {}명의 멤버의 캘린더 일정을 삭제합니다.", membersWithCalendarEvent.size());

        membersWithCalendarEvent.forEach(member -> {
            try {
                log.debug("캘린더 일정 삭제 시도: userId={}, eventId={}", member.getUser().getId(), member.getCalendarEventId());
                calendarMemberService.handleMemberLeft(member);
            } catch (Exception e) {
                log.error("그룹 삭제 시 일정 삭제 실패: userId={}, groupId={}",
                        member.getUser().getId(), group.getGroupId(), e);
            }
        });

        log.info("그룹 삭제 이벤트 처리 완료: 총 {}명의 멤버 처리", membersWithCalendarEvent.size());
    }

    /**
     * 이벤트 클래스들
     */
    public static class GroupMemberStatusChangeEvent {
        private final GroupMember groupMember;

        public GroupMemberStatusChangeEvent(GroupMember groupMember) {
            this.groupMember = groupMember;
        }

        public GroupMember getGroupMember() { return groupMember; }
    }
    public static class GroupMemberAlarmChangeEvent {
        private final GroupMember groupMember;

        public GroupMemberAlarmChangeEvent(GroupMember groupMember) {
            this.groupMember = groupMember;
        }

        public GroupMember getGroupMember() { return groupMember; }
    }

    public static class GroupInfoUpdateEvent {
        private final Group group;

        public GroupInfoUpdateEvent(Group group) {
            this.group = group;
        }

        public Group getGroup() { return group; }
    }

    public static class GroupDeletionEvent {
        private final Group group;

        public GroupDeletionEvent(Group group) {
            this.group = group;
        }

        public Group getGroup() { return group; }
    }
}
