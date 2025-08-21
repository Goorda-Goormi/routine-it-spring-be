package com.goormi.routine.domain.auth.dto;

import com.goormi.routine.domain.user.dto.UserResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
	private String accessToken;
	private String refreshToken;
	private UserResponse.Profile user;
	private boolean isNewUser;
}