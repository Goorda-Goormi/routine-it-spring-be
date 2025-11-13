package com.goormi.routine.domain.userActivity.service;

import com.goormi.routine.domain.group.entity.Group;
import com.goormi.routine.domain.group.entity.GroupMember;
import com.goormi.routine.domain.group.repository.GroupMemberRepository;
import com.goormi.routine.domain.group.repository.GroupRepository;
import com.goormi.routine.domain.ranking.service.RankingService;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import com.goormi.routine.domain.userActivity.dto.MonthlyAttendanceDashboardResponse;
import com.goormi.routine.domain.userActivity.dto.UserActivityRequest;
import com.goormi.routine.domain.userActivity.dto.UserActivityResponse;
import com.goormi.routine.domain.userActivity.entity.ActivityType;
import com.goormi.routine.domain.userActivity.entity.UserActivity;
import com.goormi.routine.domain.userActivity.repository.UserActivityRepository;
import com.goormi.routine.domain.personal_routines.domain.PersonalRoutine;
import com.goormi.routine.domain.personal_routines.repository.PersonalRoutineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.goormi.routine.domain.userActivity.dto.MonthlyAttendanceDashboardResponse.AttendanceDayDto;
import com.goormi.routine.domain.userActivity.dto.MonthlyAttendanceDashboardResponse.Summary;
import java.time.YearMonth;
import java.util.stream.IntStream;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class UserActivityServiceImpl implements UserActivityService{
    private final UserActivityRepository userActivityRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PersonalRoutineRepository personalRoutineRepository;
    private final RankingService rankingService;

    @Override
    public UserActivityResponse create(Long userId, UserActivityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserActivity userActivity;

        if (request.getActivityType() == ActivityType.GROUP_AUTH_COMPLETE) {
            if (request.getGroupId() == null) throw new IllegalArgumentException("GroupId is null");

            boolean exists = userActivityRepository.existsTodayGroupAuth(
                userId,
                ActivityType.GROUP_AUTH_COMPLETE,
                LocalDate.now(ZoneId.of("Asia/Seoul")),
                request.getGroupId()
            );

            Group group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("Group not found"));
            GroupMember groupMember = groupMemberRepository.findByGroupAndUser(group, user)
                    .orElseThrow(() -> new IllegalArgumentException("GroupMember not found"));

            userActivity = UserActivity.createActivity(user, groupMember, request.getImageUrl(), request.getIsPublic());

            System.out.println("exists check: " + userActivityRepository
                .findByUserIdAndActivityTypeAndActivityDateBetween(
                    userId, ActivityType.GROUP_AUTH_COMPLETE,
                    LocalDate.now(ZoneId.of("Asia/Seoul")),
                    LocalDate.now(ZoneId.of("Asia/Seoul"))
                ).stream()
                .map(a -> a.getGroupMember().getGroup().getGroupId())
                .toList()
            );

            if (!exists) {
                rankingService.updateRankingScore(userId, request.getGroupId(), 1);
            }
        }
        else if (request.getActivityType() == ActivityType.PERSONAL_ROUTINE_COMPLETE) {
            if (request.getPersonalRoutineId() == null) throw new IllegalArgumentException("PersonalRoutine Id is null");

            PersonalRoutine personalRoutine = personalRoutineRepository.findById(request.getPersonalRoutineId())
                    .orElseThrow(() -> new IllegalArgumentException("Personal Routine not found"));
            userActivity = UserActivity.createActivity(user, personalRoutine, request.getIsPublic());
        }
        else if (request.getActivityType() == ActivityType.DAILY_CHECKLIST) {
            userActivity = UserActivity.builder()
                    .user(user)
                    .activityType(ActivityType.DAILY_CHECKLIST)
                    .activityDate(LocalDate.now(ZoneId.of("Asia/Seoul")))
                    .createdAt(LocalDateTime.now())
                    .isPublic(false)
                    .build();
        } else{
          throw new  IllegalArgumentException("Invalid request");
        }

        UserActivity saved = userActivityRepository.save(userActivity);

        return convertToResponse(saved);
    }

    // private int calculateMonthlyAuthCount(Long userId) {
    //     try {
    //         // 현재 월을 yyyy-MM 형식으로 가져옴
    //         LocalDate startDate = LocalDate.now().withDayOfMonth(1);
    //         LocalDate endDate = startDate.plusMonths(1).minusDays(1);
    //
    //         return (int) userActivityRepository
    //             .countByUserIdAndActivityTypeAndCreatedAtBetween(
    //                 userId,
    //                 ActivityType.GROUP_AUTH_COMPLETE,
    //                 startDate.atStartOfDay(),
    //                 endDate.atTime(23, 59, 59)
    //             );
    //     } catch (Exception e) {
    //         return 0;
    //     }
    // }

    @Override
    public UserActivityResponse updateActivity(Long userId, UserActivityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserActivity userActivity = userActivityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("Activity not found"));

        if (!Objects.equals(user.getId(), userActivity.getUser().getId())) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }
        userActivity.updateActivity(request.getActivityType(), request.getIsPublic());
        return convertToResponse(userActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityResponse> getUserActivitiesPerDay(Long userId, LocalDate activityDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserActivity> activities = userActivityRepository.findAllByUserAndActivityDate(user, activityDate);
        return activities.stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityResponse> getImagesOfUserActivities(Long currentUserId, Long targetUserId) {
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        List<UserActivity> activities = userActivityRepository
                .findByUserIdAndImageUrlIsNotNullAndActivityTypeOrderByCreatedAtDesc(targetUserId, ActivityType.GROUP_AUTH_COMPLETE);

        boolean isOwner = targetUserId.equals(currentUserId);

        if (!isOwner) {
            // 본인이 아니면 공개된 사진만 조회
            return activities.stream()
                    .filter(UserActivity::getIsPublic)
                    .map(this::convertToResponse)
                    .toList();
        }

        return activities.stream()
                .map(this::convertToResponse)
                .toList();
    }


    private UserActivityResponse convertToResponse(UserActivity activity) {
        if (activity.getPersonalRoutine() != null) {
            return UserActivityResponse.fromPersonalActivity(activity);
        } else if (activity.getGroupMember() != null) {
            return UserActivityResponse.fromGroupActivity(activity);
        } else if (activity.getActivityType() != null) {
            return UserActivityResponse.from(activity);
        }
        // This case should not happen with consistent data
        throw new IllegalArgumentException
                ("UserActivity has neither PersonalRoutine nor GroupMember");
    }



    private static final Set<ActivityType> ATTENDANCE_TYPES =
            EnumSet.of(ActivityType.PERSONAL_ROUTINE_COMPLETE, ActivityType.GROUP_AUTH_COMPLETE);

    @Override
    @Transactional(readOnly = true)
    public boolean hasAttendanceOn(Long userId, LocalDate date) {
        return userActivityRepository.existsByUserIdAndActivityDateAndActivityTypeIn(
                userId, date, ATTENDANCE_TYPES
        );
    }


    @Override
    @Transactional(readOnly = true)
    public int getTotalAttendanceDays(Long userId, LocalDate startDate, LocalDate endDate) {
        // 기본값 처리
        LocalDate end = (endDate != null) ? endDate : LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate start = (startDate != null) ? startDate : LocalDate.of(1970, 1, 1);

        // 기간 역전 방지
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endDate must be on or after startDate");
        }

        // 기간 내 '출석 인정' 타입들의 활동을 모두 가져온 뒤, activityDate 기준으로 distinct
        List<UserActivity> records = userActivityRepository
                .findByUserIdAndActivityTypeInAndActivityDateBetween(
                        userId, ATTENDANCE_TYPES.stream().toList(), start, end
                );

        Set<LocalDate> distinctDays = new HashSet<>();
        for (UserActivity ua : records) {
            if (ua.getActivityDate() != null) {
                distinctDays.add(ua.getActivityDate());
            }
        }
        return distinctDays.size();
    }

    // === 월별 출석 대시보드 ===
    @Override
    @Transactional(readOnly = true)
    public MonthlyAttendanceDashboardResponse getMonthlyAttendanceDashboard(
            Long currentUserId, Long targetUserId, int year, int month
    ) {
        Long id = (targetUserId != null) ? targetUserId : currentUserId;

        userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // 월 범위 모든 활동
        List<UserActivity> records = userActivityRepository.findByUserIdAndActivityDateBetween(id, start, end);

        // 날짜별 타입 집계 + 타입별 카운트
        Map<LocalDate, Set<ActivityType>> typesByDay = new HashMap<>();
        int personalCount = 0;
        int groupCount = 0;
        int checklistCount = 0;

        for (UserActivity ua : records) {
            LocalDate d = ua.getActivityDate();
            if (d == null) continue;

            typesByDay.computeIfAbsent(d, k -> EnumSet.noneOf(ActivityType.class))
                    .add(ua.getActivityType());

            switch (ua.getActivityType()) {
                case PERSONAL_ROUTINE_COMPLETE -> personalCount++;
                case GROUP_AUTH_COMPLETE -> groupCount++;
                case DAILY_CHECKLIST -> checklistCount++;
                default -> {}
            }
        }

        // 1~말일까지 캘린더 구성
        List<AttendanceDayDto> calendar = IntStream.rangeClosed(1, ym.lengthOfMonth())
                .mapToObj(day -> {
                    LocalDate date = ym.atDay(day);
                    Set<ActivityType> types = typesByDay.getOrDefault(date, EnumSet.noneOf(ActivityType.class));
                    boolean attended = types.stream().anyMatch(ATTENDANCE_TYPES::contains);
                    return AttendanceDayDto.builder()
                            .date(date)
                            .attended(attended)
                            .activityTypes(types)
                            .build();
                })
                .toList();

        // 출석 인정 일자
        Set<LocalDate> attendedDates = new TreeSet<>();
        for (AttendanceDayDto day : calendar) {
            if (day.isAttended()) attendedDates.add(day.getDate());
        }

        int longestStreak = calcLongestStreak(attendedDates);
        int currentStreak = calcCurrentStreak(attendedDates);

        int totalDays = ym.lengthOfMonth();
        int attendedDays = attendedDates.size();
        double rate = totalDays == 0 ? 0.0 : (attendedDays * 100.0 / totalDays);
        double roundedRate = Math.round(rate * 10.0) / 10.0;

        Summary summary = Summary.builder()
                .totalDays(totalDays)
                .attendedDays(attendedDays)
                .attendanceRate(roundedRate)
                .longestStreak(longestStreak)
                .currentStreak(currentStreak)
                .personalRoutineCount(personalCount)
                .groupAuthCount(groupCount)
                .dailyChecklistCount(checklistCount)
                .build();

        return MonthlyAttendanceDashboardResponse.builder()
                .summary(summary)
                .calendar(calendar)
                .build();
    }

    // === [추가] streak 계산 유틸 ===
    private int calcLongestStreak(Set<LocalDate> attended) {
        if (attended.isEmpty()) return 0;
        int longest = 1, curr = 1;
        LocalDate prev = null;
        for (LocalDate d : attended) {
            if (prev != null && d.minusDays(1).equals(prev)) curr++;
            else curr = 1;
            longest = Math.max(longest, curr);
            prev = d;
        }
        return longest;
    }

    private int calcCurrentStreak(Set<LocalDate> attended) {
        if (attended.isEmpty()) return 0;
        List<LocalDate> list = new ArrayList<>(attended);
        Collections.sort(list);
        Collections.reverse(list);

        int streak = 1;
        LocalDate prev = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            LocalDate next = list.get(i);
            if (prev.minusDays(1).equals(next)) {
                streak++;
                prev = next;
            } else break;
        }
        return streak;
    }
}
