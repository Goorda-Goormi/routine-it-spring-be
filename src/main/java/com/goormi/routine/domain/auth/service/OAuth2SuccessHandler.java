package com.goormi.routine.domain.auth.service;

import com.goormi.routine.domain.calendar.service.CalendarSyncService;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "calendar.integration.enabled", havingValue = "true", matchIfMissing = true)
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final CalendarSyncService calendarSyncService; // 의존성 추가

    @Value("${app.oauth2.redirect-uri:http://localhost:3000}")
    private String redirectUri;

    @Value("${is.https.enabled}")
    private boolean isHttpsEnabled;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        
        // 카카오 ID는 Long 타입으로 반환됨
        Object idAttribute = oAuth2User.getAttribute("id");
        log.info("ID attribute type: {}, value: {}", idAttribute != null ? idAttribute.getClass().getName() : "null", idAttribute);
        
        String kakaoId;
        if (idAttribute instanceof Long) {
            kakaoId = String.valueOf(idAttribute);
        } else if (idAttribute instanceof String) {
            kakaoId = (String) idAttribute;
        } else {
            log.error("Unexpected ID type: {}", idAttribute != null ? idAttribute.getClass().getName() : "null");
            throw new IllegalStateException("카카오 ID 타입이 예상과 다릅니다: " + (idAttribute != null ? idAttribute.getClass().getName() : "null"));
        }
        
                // 사용자 조회 (이미 CustomOAuth2UserService에서 생성되었을 것임)
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다."));

        // 카카오 리프레시 토큰 저장 및 캘린더 동기화
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );

            if (authorizedClient != null) {
                String accessToken = authorizedClient.getAccessToken().getTokenValue();

                // 비동기 캘린더 동기화 호출
                calendarSyncService.syncUserCalendar(user.getId(), accessToken);

                if (authorizedClient.getRefreshToken() != null) {
                    String kakaoRefreshToken = authorizedClient.getRefreshToken().getTokenValue();
                    log.debug("획득한 카카오 리프레시 토큰: {}", kakaoRefreshToken);
                    user.updateKakaoRefreshToken(kakaoRefreshToken);
                    log.info("카카오 리프레시 토큰 저장 완료: userId={}", user.getId());
                } else {
                    log.warn("카카오 리프레시 토큰을 찾을 수 없습니다. 'offline_access' 스코프를 요청했는지 확인하세요.");
                }
            }
        }
        
        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        
        // Redis에 리프레시 토큰 저장
        tokenService.saveRefreshToken(user.getId(), refreshToken, jwtTokenProvider.getRefreshTokenExpiration());
        
        // 새로운 사용자인지 확인 (최초 로그인 시간과 현재 시간 비교)
        boolean isNewUser = user.getCreatedAt().plusMinutes(1).isAfter(java.time.LocalDateTime.now());

        
        // Refresh Token을 HttpOnly 쿠키에 저장
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(isHttpsEnabled); // HTTPS 환경에서는 true로 설정
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일

        // 크로스 도메인 허용
        String cookieHeader = String.format(
            "refreshToken=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=None; Secure",
            refreshToken,
            7 * 24 * 60 * 60
        );
        response.setHeader("Set-Cookie", cookieHeader);
        
        // Access Token과 사용자 정보는 URL 파라미터로 전달 (한글은 URL 인코딩)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("isNewUser", isNewUser)
                .queryParam("userId", user.getId())
                .encode()  // URL 인코딩 추가
                .build()
                .toUriString();
        
        log.info("OAuth2 login successful for user: {}, redirecting to: {}", user.getEmail(), redirectUri);
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}