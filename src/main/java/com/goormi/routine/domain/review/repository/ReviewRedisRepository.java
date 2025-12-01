package com.goormi.routine.domain.review.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import com.goormi.routine.domain.auth.repository.RedisRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ReviewRedisRepository {

	private final RedisRepository redisRepository;

	private static final String REVIEW_DATA_PREFIX = "review:data:";
	private static final String FAILED_REVIEW_PREFIX = "failed_review:";
	private static final int REVIEW_DATA_EXPIRE_DAYS = 90;
	private static final int FAILED_DATA_EXPIRE_DAYS = 7;

	public void saveReviewData(String userId, String monthYear, String jsonData) {
		try {
			String key = REVIEW_DATA_PREFIX + userId + ":" + monthYear;
			redisRepository.saveData(key, jsonData, REVIEW_DATA_EXPIRE_DAYS * 24 * 60 * 60);
			log.debug("회고 데이터 저장: 사용자 ID = {}, 월 = {}", userId, monthYear);
		} catch (Exception e) {
			log.error("회고 데이터 저장 실패: 사용자 ID = {}, 월 = {}", userId, monthYear, e);
			throw new RuntimeException("회고 데이터 저장 실패", e);
		}
	}

	public String getReviewData(String userId, String monthYear) {
		try {
			String key = REVIEW_DATA_PREFIX + userId + ":" + monthYear;
			return redisRepository.getData(key);
		} catch (Exception e) {
			log.error("회고 데이터 조회 실패: 사용자 ID = {}, 월 = {}", userId, monthYear, e);
			return null;
		}
	}

	public void saveFailedMessage(Long userId, String monthYear, String errorMessage) {
		try {
			String key = FAILED_REVIEW_PREFIX + monthYear + ":" + userId;
			String value = String.format("%s|%s", LocalDateTime.now(), errorMessage);
			redisRepository.saveData(key, value, FAILED_DATA_EXPIRE_DAYS * 24 * 60 * 60);
			log.debug("실패 메시지 저장: 사용자 ID = {}, 월 = {}", userId, monthYear);
		} catch (Exception e) {
			log.error("실패 메시지 저장 실패: 사용자 ID = {}, 월 = {}", userId, monthYear, e);
		}
	}

	public List<Long> getFailedUserIds(String monthYear) {
		try {
			String pattern = FAILED_REVIEW_PREFIX + monthYear + ":*";
			return redisRepository.getKeysByPattern(pattern).stream()
				.map(key -> {
					String[] parts = key.split(":");
					return Long.parseLong(parts[parts.length - 1]);
				})
				.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("실패 사용자 ID 조회 실패: 월 = {}", monthYear, e);
			return List.of();
		}
	}

	public void removeFailedMessage(Long userId, String monthYear) {
		try {
			String key = FAILED_REVIEW_PREFIX + monthYear + ":" + userId;
			redisRepository.deleteData(key);
			log.debug("실패 메시지 삭제: 사용자 ID = {}, 월 = {}", userId, monthYear);
		} catch (Exception e) {
			log.error("실패 메시지 삭제 실패: 사용자 ID = {}, 월 = {}", userId, monthYear, e);
		}
	}

	public int getFailedMessageCount(String monthYear) {
		return getFailedUserIds(monthYear).size();
	}

	public boolean hasFailedMessages(String monthYear) {
		return !getFailedUserIds(monthYear).isEmpty();
	}

	public Map<Long, String> getPreviousReviewsJsonBatch(
		List<Long> userIds, String previousMonth) {

		List<String> keys = userIds.stream()
			.map(id -> REVIEW_DATA_PREFIX + id + ":" + previousMonth)
			.collect(Collectors.toList());

		try {
			List<String> jsonList = redisRepository.getMultipleData(keys);

			if (jsonList == null || jsonList.isEmpty()) {
				return Map.of();
			}

			Map<Long, String> reviewMap = new java.util.HashMap<>();
			for (int i = 0; i < keys.size(); i++) {
				String json = jsonList.get(i);
				if (json != null) {
					Long userId = userIds.get(i);
					reviewMap.put(userId, json);
				}
			}
			return reviewMap;
		} catch (Exception e) {
			log.error("이전 회고 데이터 배치 조회 실패: 월 = {}", previousMonth, e);
			return Map.of();
		}
	}
}