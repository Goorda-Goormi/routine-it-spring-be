package com.goormi.routine.domain.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(nullable = false, length = 50, unique = true)
	private String nickname;

	@Column(name = "profile_message", columnDefinition = "TEXT")
	private String profileMessage;

	@Column(name = "profile_image_url")
	private String profileImageUrl;

	@Column(name = "is_alarm_on", nullable = false)
	private Boolean isAlarmOn = true;

	@Column(name = "is_dark_mode", nullable = false)
	private Boolean isDarkMode = false;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}