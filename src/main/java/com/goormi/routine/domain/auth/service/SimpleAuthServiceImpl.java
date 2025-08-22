package com.goormi.routine.domain.auth.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormi.routine.domain.auth.dto.AuthResponse;
import com.goormi.routine.domain.auth.dto.TokenResponse;
import com.goormi.routine.domain.auth.entity.AuthAccount;
import com.goormi.routine.domain.auth.repository.AuthAccountRepository;
import com.goormi.routine.domain.user.dto.UserResponse;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SimpleAuthServiceImpl implements SimpleAuthService {

	private final UserService userService;
	private final AuthAccountRepository authAccountRepository;

	@Override
	@Transactional
	public AuthResponse processKakaoLogin(String code) {
		// 임시 구현
		String kakaoId = "kakao_" + code;
		String nickname = "테스트유저" + System.currentTimeMillis() % 1000;

		Optional<AuthAccount> existingAccount = authAccountRepository.findByKakaoId(kakaoId);

		User user;
		boolean isNewUser;

		if (existingAccount.isPresent()) {
			user = existingAccount.get().getUser();
			isNewUser = false;
		} else {
			user = userService.createUserWithKakao(nickname, kakaoId);
			isNewUser = true;
		}

		TokenResponse tokens = userService.generateTokens(user.getUserId(), user.getNickname());

		return AuthResponse.builder()
			.accessToken(tokens.getAccessToken())
			.refreshToken(tokens.getRefreshToken())
			.user(UserResponse.Profile.from(user))
			.isNewUser(isNewUser)
			.build();
	}
}