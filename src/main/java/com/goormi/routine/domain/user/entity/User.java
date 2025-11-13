package com.goormi.routine.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String kakaoId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String nickname;

    @Column(name = "profile_image_url", columnDefinition = "TEXT", length = 255)
    private String profileImageUrl;

    @Column(name = "profile_message", columnDefinition = "TEXT")
    private String profileMessage;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken; // JWT RefreshToken (Redis 대신 DB 백업용)
    
    @Column(name = "kakao_refresh_token", columnDefinition = "TEXT")
    private String kakaoRefreshToken; // 카카오 API용 RefreshToken
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    //카카오 캘린더 연동 관련 필드
    @Column(name = "calendar_connected")
    @Builder.Default
    private Boolean calendarConnected = false;

    public void createProfile(String nickname, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }
    
    public void updateProfile(String nickname, String profileMessage, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (profileMessage != null) {
            this.profileMessage = profileMessage;
        }
    }
    
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public void updateKakaoRefreshToken(String kakaoRefreshToken) {
        this.kakaoRefreshToken = kakaoRefreshToken;
    }

    public void connectCalendar() {
        this.calendarConnected = true;
    }
    public void disconnectCalendar() {
        this.calendarConnected = false;
    }
    /**
     * 기존 로직에 캘린더 연동 해제 추가
     */
    public void deactivateAccount() {
        this.active = false;
        this.calendarConnected = false;
        this.refreshToken = null;
    }
    
    public static User createKakaoUser(String kakaoId, String email, String nickname, String profileImageUrl) {
        return User.builder()
                .kakaoId(kakaoId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .role(UserRole.USER)
                .build();
    }

    public enum UserRole {
        USER, ADMIN
    }
}