package com.goormi.routine.domain.auth.controller;

import com.goormi.routine.common.response.ApiResponse;
import com.goormi.routine.domain.auth.dto.*;
import com.goormi.routine.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "인증", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @Operation(summary = "닉네임 중복 체크", description = "닉네임 중복 여부를 확인합니다")
    @PostMapping("/check-nickname")
    public ApiResponse<Boolean> checkNickname(@RequestBody NicknameCheckRequest request) {
        boolean isAvailable = authService.isNicknameAvailable(request.nickname());
        return ApiResponse.success(isAvailable);
    }
    
    @Operation(summary = "회원가입 완료", description = "닉네임을 설정하여 회원가입을 완료합니다")
    @PostMapping("/signup")
    public ApiResponse<UserInfoResponse> completeSignup(
            @AuthenticationPrincipal Long userId,
            @RequestBody SignupRequest request
    ) {
        UserInfoResponse response = authService.completeSignup(userId, request.nickname());
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다")
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
        UserInfoResponse response = authService.getUserInfo(userId);
        return ApiResponse.success(response);
    }
    
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 쿠키에서 refresh token 추출
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }
        
        if (refreshToken == null) {
            throw new IllegalArgumentException("리프레시 토큰이 없습니다.");
        }
        
        TokenResponse tokenResponse = authService.refreshToken(refreshToken);
        
        // 새로운 refresh token을 쿠키에 저장
        Cookie newRefreshTokenCookie = new Cookie("refreshToken", tokenResponse.refreshToken());
        newRefreshTokenCookie.setHttpOnly(true);
        newRefreshTokenCookie.setSecure(false); // HTTPS 환경에서는 true로 설정
        newRefreshTokenCookie.setPath("/");
        newRefreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(newRefreshTokenCookie);
        
        return ApiResponse.success(tokenResponse);
    }
    
    @Operation(summary = "로그아웃", description = "현재 사용자를 로그아웃합니다")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String token = extractToken(request);
        authService.logout(userId, token);
        
        // Refresh Token 쿠키 삭제
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0); // 즉시 만료
        response.addCookie(refreshTokenCookie);
        
        log.info("User {} logged out successfully", userId);
        return ApiResponse.success(null);
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}