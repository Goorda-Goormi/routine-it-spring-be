package com.goormi.routine.domain.calendar.service;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * 캘린더와 그룹 멤버 간의 연동을 처리하는 트랜잭션 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
public class CalendarMemberService {

    private final CalendarService calendarService;
    private final GroupMemberRepository groupMemberRepository;

    /**
     * 그룹 멤버 가입 시 캘린더 일정 생성 (트랜잭션 적용)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMemberJoined(GroupMember groupMember) {
        try {
            Long userId = groupMember.getUser().getId();
            Group group = groupMember.getGroup();

            // 캘린더 연동 여부 확인
            if (!calendarService.isCalendarConnected(userId)) {
                log.info("캘린더가 연동되지 않은 사용자입니다. 그룹 일정 생성을 건너뜁니다: userId={}", userId);
                return;
            }

            // 그룹 일정 생성
            log.debug("그룹 일정 생성 시작: userId={}, groupId={}", userId, group.getGroupId());
            String eventId = calendarService.createGroupSchedule(userId, group);

            if (eventId == null || eventId.trim().isEmpty()) {
                log.error("캘린더 일정 생성 실패: eventId가 null 또는 공백입니다");
                throw new IllegalStateException("캘린더 일정 생성 실패: eventId가 유효하지 않습니다");
            }

            // GroupMember에 eventId 저장
            // 전달받은 groupMember가 영속 상태가 아닐 수 있으므로, 필요시 findById로 영속 엔티티를 가져와야 함
            GroupMember managedGroupMember = groupMember.getMemberId() == null ? groupMember :
                    groupMemberRepository.findById(groupMember.getMemberId())
                            .orElseThrow(() -> new IllegalStateException("GroupMember를 찾을 수 없습니다. ID: " + groupMember.getMemberId()));

            managedGroupMember.updateCalendarEventId(eventId);
            groupMemberRepository.save(managedGroupMember);

            log.info("그룹 일정 생성 및 GroupMember에 eventId 저장 완료: userId={}, groupId={}, eventId={}",
                    userId, group.getGroupId(), eventId);

        } catch (Exception e) {
            log.error("그룹 일정 생성 중 예외 발생: groupMemberId={}, 에러: {}", groupMember.getMemberId(), e.getMessage(), e);
            throw e; // @Transactional 롤백을 위해 예외 재발생
        }
    }

    /**
     * 그룹 멤버 탈퇴 시 캘린더 일정 삭제 (트랜잭션 적용)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMemberLeft(GroupMember groupMember) {
        try {
            Long userId = groupMember.getUser().getId();
            String eventId = groupMember.getCalendarEventId();

            log.info("그룹 멤버 탈퇴 처리 시작: userId={}, eventId={}", userId, eventId);

            // 캘린더 이벤트가 있는 경우에만 삭제
            if (eventId != null && !eventId.trim().isEmpty() && calendarService.isCalendarConnected(userId)) {
                calendarService.deleteGroupSchedule(eventId, userId);

                // GroupMember 처리
                GroupMember managedGroupMember = groupMember.getMemberId() == null ? groupMember :
                        groupMemberRepository.findById(groupMember.getMemberId())
                                .orElseThrow(() -> new IllegalStateException("GroupMember를 찾을 수 없습니다. ID: " + groupMember.getMemberId()));

                managedGroupMember.clearCalendarEventId();
                groupMemberRepository.save(managedGroupMember);

                log.info("그룹 일정 삭제 및 GroupMember의 eventId 제거 완료: userId={}, eventId={}", userId, eventId);
            } else {
                log.debug("삭제할 캘린더 이벤트가 없거나 캘린더가 연동되지 않았습니다: userId={}, eventId={}", userId, eventId);
            }

        } catch (Exception e) {
            log.error("그룹 멤버 탈퇴 처리 중 예외 발생: userId={}", groupMember.getUser().getId(), e);
            throw e; // @Transactional 롤백을 위해 예외 재발생
        }
    }
}
