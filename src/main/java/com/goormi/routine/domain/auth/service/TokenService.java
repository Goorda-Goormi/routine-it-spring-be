package com.goormi.routine.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.goormi.routine.domain.auth.repository.RedisRepository;

@Service
@RequiredArgsConstructor
public class TokenService {

	private final RedisRepository redisRepository;

	public void saveRefreshToken(Long userId, String refreshToken, long duration) {
		redisRepository.saveRefreshToken(String.valueOf(userId), refreshToken, duration);
	}

	public String getRefreshToken(Long userId) {
		return redisRepository.getRefreshToken(String.valueOf(userId));
	}

	public void deleteRefreshToken(Long userId) {
		redisRepository.deleteRefreshToken(String.valueOf(userId));
	}

	public void addToBlacklist(String accessToken, long duration) {
		redisRepository.saveBlackList(accessToken, duration);
	}

	public boolean isBlacklisted(String accessToken) {
		return redisRepository.isBlackListed(accessToken);
	}
}
