package com.goormi.routine.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormi.routine.common.util.JwtUtil;
import com.goormi.routine.domain.auth.dto.TokenResponse;
import com.goormi.routine.domain.auth.entity.AuthAccount;
import com.goormi.routine.domain.auth.repository.AuthAccountRepository;
import com.goormi.routine.domain.auth.service.TokenService;
import com.goormi.routine.domain.user.dto.UserRequest;
import com.goormi.routine.domain.user.dto.UserResponse;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.exception.DuplicateNicknameException;
import com.goormi.routine.domain.user.exception.UserNotFoundException;
import com.goormi.routine.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final AuthAccountRepository authAccountRepository;
	private final TokenService tokenService;
	private final JwtUtil jwtUtil;

	@Override
	public User findById(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new UserNotFoundException(userId));
	}

	@Override
	public UserResponse.Profile getMyProfile(Long userId) {
		User user = findById(userId);
		return UserResponse.Profile.from(user);
	}

	@Override
	public UserResponse.PublicProfile getPublicProfile(Long userId) {
		User user = findById(userId);
		return UserResponse.PublicProfile.from(user);
	}

	@Override
	@Transactional
	public UserResponse.Profile updateProfile(Long userId, UserRequest.UpdateProfile request) {
		User user = findById(userId);

		if (!user.getNickname().equals(request.getNickname()) &&
			userRepository.existsByNickname(request.getNickname())) {
			throw new DuplicateNicknameException(request.getNickname());
		}

		user.updateProfile(request.getNickname(), request.getProfileMessage(), request.getProfileImageUrl());
		userRepository.save(user);

		return UserResponse.Profile.from(user);
	}

	@Override
	@Transactional
	public UserResponse.Profile updateSettings(Long userId, UserRequest.UpdateSettings request) {
		User user = findById(userId);
		user.updateSettings(request.getIsAlarmOn(), request.getIsDarkMode());
		userRepository.save(user);

		return UserResponse.Profile.from(user);
	}

	@Override
	@Transactional
	public User createUserWithKakao(String kakaoNickname, String kakaoId) {
		String uniqueNickname = generateUniqueNickname(kakaoNickname);

		User user = User.builder()
			.nickname(uniqueNickname)
			.profileMessage("안녕하세요! " + uniqueNickname + "입니다.")
			.build();
		user = userRepository.save(user);

		AuthAccount authAccount = AuthAccount.builder()
			.user(user)
			.kakaoId(kakaoId)
			.build();
		authAccountRepository.save(authAccount);

		return user;
	}

	private String generateUniqueNickname(String originalNickname) {
		String nickname = originalNickname;
		int counter = 1;
		while (userRepository.existsByNickname(nickname)) {
			nickname = originalNickname + counter;
			counter++;
		}
		return nickname;
	}

	@Override
	public boolean isNicknameExists(String nickname) {
		return userRepository.existsByNickname(nickname);
	}

	@Override
	@Transactional
	public TokenResponse generateTokens(Long userId, String nickname) {
		String accessToken = jwtUtil.generateAccessToken(userId, nickname);
		String refreshToken = jwtUtil.generateRefreshToken(userId);

		AuthAccount authAccount = authAccountRepository.findByUser_UserId(userId)
			.orElseThrow(() -> new UserNotFoundException("인증 계정을 찾을 수 없습니다"));
		authAccount.updateRefreshToken(refreshToken);
		authAccountRepository.save(authAccount);

		tokenService.saveRefreshToken(userId, refreshToken, jwtUtil.getRefreshTokenValidity());

		return TokenResponse.builder()
			.accessToken(accessToken)
			.refreshToken(refreshToken)
			.build();
	}

	@Override
	@Transactional
	public void clearRefreshToken(Long userId) {
		AuthAccount authAccount = authAccountRepository.findByUser_UserId(userId)
			.orElseThrow(() -> new UserNotFoundException("인증 계정을 찾을 수 없습니다"));
		authAccount.clearRefreshToken();
		authAccountRepository.save(authAccount);

		tokenService.deleteRefreshToken(userId);
	}

	@Override
	@Transactional
	public void deactivateUser(Long userId) {
		AuthAccount authAccount = authAccountRepository.findByUser_UserId(userId)
			.orElseThrow(() -> new UserNotFoundException("인증 계정을 찾을 수 없습니다"));
		authAccount.deactivate();
		authAccount.clearRefreshToken();
		authAccountRepository.save(authAccount);

		tokenService.deleteRefreshToken(userId);
	}
}