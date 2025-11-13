package com.goormi.routine.domain.group.entity;

import com.goormi.routine.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_group")
public class Group {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private User leader;

    @OneToMany(mappedBy = "group",cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<GroupMember> groupMembers = new ArrayList<>();

    @Column(nullable = false)
    private String groupName;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupType groupType;


    private LocalTime alarmTime;

    @Column(length = 7)
    private String authDays; // ex) 월, 수, 금 -> 0101010 (일 - 토)


    @Column(length = 50)
    private String category;

    private String groupImageUrl;

    private int maxMembers;
    private int currentMemberCnt;


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean isActive;

    private Boolean isAlarm;


    // 생성자
    @Builder
    private Group(User leader, String groupName, String description,
                  GroupType groupType, LocalTime alarmTime, String authDays,
                  String category, String groupImageUrl, Integer maxMembers, Boolean isAlarm ) {
        //basicInfo
        this.leader = leader;
        this.groupName = groupName;
        this.description = description;
        this.groupType = groupType;
        //timeInfo
        this.alarmTime = alarmTime;
        this.authDays = authDays;
        //optionalInfo
        this.category = category;
        this.groupImageUrl = groupImageUrl;
        this.maxMembers = maxMembers;
        this.isAlarm = isAlarm == null ? false : isAlarm;
    }

    // 생성 초기값은 여기서 세팅
    public void setInitialValues(Group group) {

        group.createdAt = LocalDateTime.now();
        group.updatedAt = group.createdAt;
        group.isActive = true;
    }

    public GroupMember addMember(User user) {
        GroupMember groupMember = GroupMember.createGroupMember
                (this, user, GroupMemberRole.MEMBER, GroupMemberStatus.PENDING);
        this.groupMembers.add(groupMember);
        return groupMember;
    }

    public void addLeader(User user) {
        GroupMember groupLeader = GroupMember.createGroupMember
                (this, user, GroupMemberRole.LEADER,  GroupMemberStatus.JOINED);
        addMemberCnt();
        this.groupMembers.add(groupLeader);
    }

    public void addMemberCnt() {
        if (this.getCurrentMemberCnt() >= this.getMaxMembers()) {
            throw new IllegalArgumentException("최대 멤버수를 초과할 수 없습니다.");
        }
        this.currentMemberCnt++;
    }
    public void minusMemberCnt() {
        this.currentMemberCnt--;
    }

    public void changeLeader(User user){
        this.leader = user;
    }

    public void updateBasicInfo(String name, String description, GroupType groupType) {
        if (name == null || name.isBlank()) {
            throw new  IllegalArgumentException("name cannot be null or blank");
            // throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        this.groupName = name;
        this.description = description;
        this.groupType = groupType;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTimeInfo(LocalTime alarmTime, String authDays) {
        this.alarmTime =  alarmTime;
        this.authDays = authDays;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateOtherInfo(String category, String groupImageUrl, int maxMembers) {
        this.category = category;
        this.groupImageUrl = groupImageUrl;
        this.maxMembers = maxMembers;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateIsAlarm(Boolean isAlarm) {
        this.isAlarm = isAlarm == null ? false : isAlarm;
    }


    public void activate() {
        this.isActive = true;
    }
    public void deactivate() {
        this.isActive = false;
    }


}
