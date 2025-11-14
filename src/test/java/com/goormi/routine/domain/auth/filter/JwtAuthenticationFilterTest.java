package com.goormi.routine.domain.auth.filter;

import com.goormi.routine.domain.auth.service.JwtTokenProvider;
import com.goormi.routine.domain.auth.service.TokenService;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * JwtAuthenticationFilter 단위 테스트
 *
 * JWT 토큰 검증 필터의 동작을 검증합니다:
 * 1. Public 엔드포인트는 인증 없이 통과
 * 2. 유효한 JWT 토큰 검증 및 인증 설정
 * 3. 만료된 토큰 처리
 * 4. 유효하지 않은 토큰 처리
 * 5. 블랙리스트 토큰 처리
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter writer;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        testUser = User.createKakaoUser(
                "123456789",
                "test@kakao.com",
                "테스트유저",
                "http://example.com/profile.jpg"
        );
        // ID와 Role 설정을 위해 Reflection 사용
        org.springframework.test.util.ReflectionTestUtils.setField(testUser, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(testUser, "role", User.UserRole.USER);

        validToken = "valid-jwt-token";
    }

    @Test
    @DisplayName("Public 엔드포인트 - 인증 없이 통과")
    void doFilterInternal_PublicEndpoint_PassesWithoutAuthentication() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/health");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 JWT 토큰 - 인증 성공")
    void doFilterInternal_ValidToken_AuthenticatesUser() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/users/me");
        given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);

        given(jwtTokenProvider.validateTokenWithDetails(validToken))
                .willReturn(JwtTokenProvider.TokenValidationResult.VALID);

        given(jwtTokenProvider.getUserId(validToken)).willReturn(testUser.getId());

        given(tokenService.isBlacklisted(validToken)).willReturn(false);

        given(userRepository.findById(testUser.getId()))
                .willReturn(Optional.of(testUser));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).validateTokenWithDetails(validToken);
        verify(tokenService).isBlacklisted(validToken);
        verify(userRepository).findById(testUser.getId());

        // SecurityContext에 인증 정보가 설정되었는지 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("토큰 없음 - 필터 통과 (EntryPoint에서 처리)")
    void doFilterInternal_NoToken_PassesToEntryPoint() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/users/me");
        given(request.getHeader("Authorization")).willReturn(null);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("만료된 토큰 - 401 에러 반환")
    void doFilterInternal_ExpiredToken_Returns401() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/users/me");
        given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
        given(response.getWriter()).willReturn(writer);

        given(jwtTokenProvider.validateTokenWithDetails(validToken))
                .willReturn(JwtTokenProvider.TokenValidationResult.EXPIRED);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(response).getWriter();
        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 토큰 - 401 에러 반환")
    void doFilterInternal_InvalidToken_Returns401() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/users/me");
        given(request.getHeader("Authorization")).willReturn("Bearer invalid-token");
        given(response.getWriter()).willReturn(writer);

        given(jwtTokenProvider.validateTokenWithDetails("invalid-token"))
                .willReturn(JwtTokenProvider.TokenValidationResult.INVALID);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("블랙리스트 토큰 - 401 에러 반환")
    void doFilterInternal_BlacklistedToken_Returns401() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/users/me");
        given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
        given(response.getWriter()).willReturn(writer);

        given(jwtTokenProvider.validateTokenWithDetails(validToken))
                .willReturn(JwtTokenProvider.TokenValidationResult.VALID);

        given(tokenService.isBlacklisted(validToken)).willReturn(true);

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(tokenService).isBlacklisted(validToken);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("사용자 없음 - 401 에러 반환")
    void doFilterInternal_UserNotFound_Returns401() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/users/me");
        given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
        given(response.getWriter()).willReturn(writer);

        given(jwtTokenProvider.validateTokenWithDetails(validToken))
                .willReturn(JwtTokenProvider.TokenValidationResult.VALID);

        given(jwtTokenProvider.getUserId(validToken)).willReturn(999L);

        given(tokenService.isBlacklisted(validToken)).willReturn(false);

        given(userRepository.findById(999L))
                .willReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(userRepository).findById(999L);
        verify(filterChain, never()).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer 없는 토큰 헤더 - 필터 통과")
    void doFilterInternal_TokenWithoutBearer_PassesToEntryPoint() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/users/me");
        given(request.getHeader("Authorization")).willReturn("invalid-format-token");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("OAuth2 엔드포인트 - 인증 없이 통과")
    void doFilterInternal_OAuth2Endpoint_PassesWithoutAuthentication() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/oauth2/authorization/kakao");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Swagger 엔드포인트 - 인증 없이 통과")
    void doFilterInternal_SwaggerEndpoint_PassesWithoutAuthentication() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/swagger-ui/index.html");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("토큰 갱신 엔드포인트 - 인증 없이 통과")
    void doFilterInternal_RefreshEndpoint_PassesWithoutAuthentication() throws ServletException, IOException {
        // given
        given(request.getRequestURI()).willReturn("/api/auth/refresh");

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}