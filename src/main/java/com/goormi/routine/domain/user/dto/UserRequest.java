package com.goormi.routine.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequest {

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UpdateProfile {
		@NotBlank(message = "닉네임은 필수입니다")
		@Size(min = 2, max = 50, message = "닉네임은 2-50자여야 합니다")
		private String nickname;

		@Size(max = 500, message = "자기소개는 500자 이하여야 합니다")
		private String profileMessage;

		@Size(max = 255, message = "프로필 이미지 URL은 255자 이하여야 합니다")
		private String profileImageUrl;
	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UpdateSettings {
		@NotNull(message = "알림 설정은 필수입니다")
		private Boolean isAlarmOn;

		@NotNull(message = "다크모드 설정은 필수입니다")
		private Boolean isDarkMode;
	}
}