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
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(unique = true, nullable = false)
    private String kakaoId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false, length = 50, unique = true)
    private String nickname;
    
    @Column(name = "profile_message", columnDefinition = "TEXT")
    private String profileMessage;
    
    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;
    
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
    
    @Builder.Default
    @Column(name = "is_alarm_on", nullable = false)
    private Boolean isAlarmOn = true;
    
    @Builder.Default
    @Column(name = "is_dark_mode", nullable = false)
    private Boolean isDarkMode = false;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public void updateProfile(String nickname, String profileMessage, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileMessage != null) {
            this.profileMessage = profileMessage;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }
    
    public void updateSettings(Boolean isAlarmOn, Boolean isDarkMode) {
        if (isAlarmOn != null) {
            this.isAlarmOn = isAlarmOn;
        }
        if (isDarkMode != null) {
            this.isDarkMode = isDarkMode;
        }
    }
    
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public static User createKakaoUser(String kakaoId, String email, String nickname, String profileImageUrl) {
        return User.builder()
                .kakaoId(kakaoId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .role(UserRole.USER)
                .isAlarmOn(true)
                .isDarkMode(false)
                .build();
    }
    
    public enum UserRole {
        USER, ADMIN
    }
}