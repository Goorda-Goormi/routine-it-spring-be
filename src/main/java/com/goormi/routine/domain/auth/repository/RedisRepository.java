package com.goormi.routine.domain.auth.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class RedisRepository {
	private final StringRedisTemplate redisTemplate;

	public RedisRepository(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void saveRefreshToken(String userId, String token, long duration) {
		redisTemplate.opsForValue().set("RT:" + userId, token, duration, TimeUnit.MILLISECONDS);
	}

	public String getRefreshToken(String userId) {
		return redisTemplate.opsForValue().get("RT:" + userId);
	}

	public void deleteRefreshToken(String userId) {
		redisTemplate.delete("RT:" + userId);
	}

	public void saveBlackList(String token, long duration) {
		redisTemplate.opsForValue().set("BL:" + token, "blacklisted", duration, TimeUnit.MILLISECONDS);
	}

	public boolean isBlackListed(String token) {
		return redisTemplate.hasKey("BL:" + token);
	}

	// 데이터 저장/조회용
	public void saveData(String key, String value, long expireSeconds) {
		if (expireSeconds > 0) {
			redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(expireSeconds));
		} else {
			redisTemplate.opsForValue().set(key, value);
		}
	}

	public String getData(String key) {
		Object value = redisTemplate.opsForValue().get(key);
		return value != null ? value.toString() : null;
	}

	public void deleteData(String key) {
		redisTemplate.delete(key);
	}

	public List<String> getKeysByPattern(String pattern) {
		Set<String> keys = redisTemplate.keys(pattern);
		return keys != null ? keys.stream().collect(Collectors.toList()) : List.of();
	}

	public boolean hasKey(String key) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key));
	}

	public void setExpire(String key, long seconds) {
		redisTemplate.expire(key, Duration.ofSeconds(seconds));
	}

	public List<String> getMultipleData(List<String> keys) {
		if (keys == null || keys.isEmpty()) {
			return List.of();
		}
		return redisTemplate.opsForValue().multiGet(keys);
	}
}
