package com.goormi.routine.domain.auth.entity;

import com.goormi.routine.domain.user.entity.User;
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
@Table(name = "auth_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthAccount {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "auth_id")
	private Long authId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "kakao_id", nullable = false, unique = true)
	private String kakaoId;

	@Column(name = "refresh_token")
	private String refreshToken;

	@Builder.Default
	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public void deactivate() {
		this.isActive = false;
	}

	public void updateRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public void clearRefreshToken() {
		this.refreshToken = null;
	}
}