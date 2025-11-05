package com.goormi.routine.domain.calendar.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.TimeUnit;

/**
 * 카카오 토큰 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
public class KakaoTokenService {

    private static final String KAKAO_ACCESS_TOKEN_PREFIX = "kakao_access_token_";
    private final WebClient webClient;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    /**
     * 카카오 Access Token 조회 (캐싱 적용)
     */
    @Transactional
    public String getKakaoAccessTokenByUserId(Long userId) {
        log.info("=== 카카오 액세스 토큰 조회 시작: userId={} ===", userId);

        // 1. Redis 캐시에서 액세스 토큰 조회
        String cacheKey = KAKAO_ACCESS_TOKEN_PREFIX + userId;
        String cachedToken = redisTemplate.opsForValue().get(cacheKey);

        if (cachedToken != null) {
            log.info("캐시에서 카카오 액세스 토큰 발견. 캐시된 토큰을 사용합니다. userId={}", userId);
            return cachedToken;
        }

        log.info("캐시에 토큰이 없음. DB에서 리프레시 토큰을 조회하여 새로 발급합니다. userId={}", userId);

        // 2. 캐시에 토큰이 없으면 DB에서 리프레시 토큰 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        if (user.getKakaoRefreshToken() == null) {
            log.warn("저장된 카카오 리프레시 토큰이 없습니다. 카카오 캘린더 연동을 건너뜁니다: userId={}", userId);
            return null;
        }

        // 3. 리프레시 토큰으로 새로운 액세스 토큰 발급
        TokenResponse tokenResponse = refreshKakaoAccessToken(user.getKakaoRefreshToken());

        // 4. 새로 발급받은 토큰을 캐시에 저장
        if (tokenResponse != null && tokenResponse.accessToken != null) {
            // 카카오에서 받은 만료 시간(초)에서 5분의 버퍼를 둠
            long expiresIn = tokenResponse.expiresIn - 300;
            if (expiresIn > 0) {
                redisTemplate.opsForValue().set(cacheKey, tokenResponse.accessToken, expiresIn, TimeUnit.SECONDS);
                log.info("새로운 카카오 액세스 토큰을 캐시에 저장했습니다. userId={}, expiresIn={}s", userId, expiresIn);
            }

            // 새로운 리프레시 토큰이 발급된 경우, 데이터베이스에 업데이트
            if (tokenResponse.refreshToken != null) {
                log.info("카카오 리프레시 토큰 갱신: userId={}", userId);
                user.updateKakaoRefreshToken(tokenResponse.refreshToken);
            }

            log.info("카카오 액세스 토큰 조회 완료: userId={}", userId);
            return tokenResponse.accessToken;
        }

        log.error("카카오 액세스 토큰 발급에 실패했습니다. userId={}", userId);
        return null;
    }

    /**
     * 카카오 Refresh Token으로 Access Token 갱신
     */
    private TokenResponse refreshKakaoAccessToken(String kakaoRefreshToken) {
        log.info("=== 카카오 액세스 토큰 갱신 시작 ===");
        try {
            return webClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .bodyValue(buildRefreshTokenRequest(kakaoRefreshToken))
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("카카오 액세스 토큰 갱신 중 API 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("카카오 토큰 갱신에 실패했습니다", e);
        }
    }

    private String buildRefreshTokenRequest(String refreshToken) {
        return String.format(
                "grant_type=refresh_token&client_id=%s&client_secret=%s&refresh_token=%s",
                clientId, clientSecret, refreshToken
        );
    }

    public static class TokenResponse {
        @JsonProperty("access_token")
        public String accessToken;

        @JsonProperty("refresh_token")
        public String refreshToken;

        @JsonProperty("expires_in")
        public Integer expiresIn;

        @JsonProperty("refresh_token_expires_in")
        public Integer refreshTokenExpiresIn;

        @Override
        public String toString() {
            return "TokenResponse{"
                    + "accessToken='" + (accessToken != null ? "***" : "null") + "'"
                    + ", refreshToken='" + (refreshToken != null ? "***" : "null") + "'"
                    + ", expiresIn=" + expiresIn
                    + ", refreshTokenExpiresIn=" + refreshTokenExpiresIn
                    + "}";
        }
    }
}
