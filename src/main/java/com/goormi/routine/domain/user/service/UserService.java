package com.goormi.routine.domain.user.service;

import com.goormi.routine.domain.auth.dto.TokenResponse;
import com.goormi.routine.domain.user.dto.UserRequest;
import com.goormi.routine.domain.user.dto.UserResponse;
import com.goormi.routine.domain.user.entity.User;

public interface UserService {

	User findById(Long userId);

	UserResponse.Profile getMyProfile(Long userId);

	UserResponse.PublicProfile getPublicProfile(Long userId);

	UserResponse.Profile updateProfile(Long userId, UserRequest.UpdateProfile request);

	UserResponse.Profile updateSettings(Long userId, UserRequest.UpdateSettings request);

	User createUserWithKakao(String kakaoNickname, String kakaoId);

	boolean isNicknameExists(String nickname);

	TokenResponse generateTokens(Long userId, String nickname);

	void clearRefreshToken(Long userId);

	void deactivateUser(Long userId);
}