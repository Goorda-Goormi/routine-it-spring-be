package com.goormi.routine.domain.user.dto;

import java.time.LocalDateTime;

import com.goormi.routine.domain.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class Profile {
		private Long userId;
		private String nickname;
		private String profileMessage;
		private String profileImageUrl;
		private Boolean isAlarmOn;
		private Boolean isDarkMode;
		private LocalDateTime createdAt;
		private LocalDateTime updatedAt;

		public static Profile from(User user) {
			return Profile.builder()
				.userId(user.getUserId())
				.nickname(user.getNickname())
				.profileMessage(user.getProfileMessage())
				.profileImageUrl(user.getProfileImageUrl())
				.isAlarmOn(user.getIsAlarmOn())
				.isDarkMode(user.getIsDarkMode())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.build();
		}
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class PublicProfile {
		private Long userId;
		private String nickname;
		private String profileMessage;
		private String profileImageUrl;
		private LocalDateTime createdAt;

		public static PublicProfile from(User user) {
			return PublicProfile.builder()
				.userId(user.getUserId())
				.nickname(user.getNickname())
				.profileMessage(user.getProfileMessage())
				.profileImageUrl(user.getProfileImageUrl())
				.createdAt(user.getCreatedAt())
				.build();
		}
	}
}