package com.goormi.routine.domain.settings.service;

import com.goormi.routine.domain.settings.dto.SettingsResponse;
import com.goormi.routine.domain.settings.entity.UserSettings;
import com.goormi.routine.domain.settings.repository.UserSettingsRepository;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SettingsServiceImpl implements SettingsService {

	private final UserRepository userRepository;
	private final UserSettingsRepository userSettingsRepository;

	@Override
	@Transactional(readOnly = true)
	public SettingsResponse getSettings(Long userId) {
		validateUserExists(userId);
		UserSettings settings = getOrCreateSettings(userId);
		return toResponse(settings);
	}

	@Override
	public SettingsResponse resetSettings(Long userId) {
		validateUserExists(userId);

		UserSettings settings = getOrCreateSettings(userId);
		settings.updateSettings(true, false);
		userSettingsRepository.save(settings);

		return toResponse(settings);
	}

	@Override
	public Boolean updateAlarmSetting(Long userId, Boolean isAlarmOn) {
		validateUserExists(userId);

		if (isAlarmOn == null) {
			throw new IllegalArgumentException("알림 설정 값은 null일 수 없습니다.");
		}

		UserSettings settings = getOrCreateSettings(userId);
		settings.updateAlarmSetting(isAlarmOn);
		userSettingsRepository.save(settings);

		return settings.getIsAlarmOn();
	}

	@Override
	public Boolean updateDarkModeSetting(Long userId, Boolean isDarkMode) {
		validateUserExists(userId);

		if (isDarkMode == null) {
			throw new IllegalArgumentException("다크모드 설정 값은 null일 수 없습니다.");
		}

		UserSettings settings = getOrCreateSettings(userId);
		settings.updateDarkModeSetting(isDarkMode);
		userSettingsRepository.save(settings);

		return settings.getIsDarkMode();
	}

	private void validateUserExists(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		if (!user.isActive()) {
			throw new IllegalArgumentException("비활성화된 사용자입니다.");
		}
	}

	private UserSettings getOrCreateSettings(Long userId) {
		return userSettingsRepository.findByUserId(userId)
			.orElseGet(() -> {
				UserSettings settings = UserSettings.createDefault(userId);
				return userSettingsRepository.save(settings);
			});
	}

	private SettingsResponse toResponse(UserSettings settings) {
		return SettingsResponse.builder()
			.userId(settings.getUserId())
			.isAlarmOn(settings.getIsAlarmOn())
			.isDarkMode(settings.getIsDarkMode())
			.build();
	}
}