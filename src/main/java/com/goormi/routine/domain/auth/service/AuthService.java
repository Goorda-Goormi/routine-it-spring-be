package com.goormi.routine.domain.auth.service;

import com.goormi.routine.domain.auth.dto.TokenResponse;
import com.goormi.routine.domain.auth.dto.UserInfoResponse;
import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }
    
    @Transactional
    public UserInfoResponse completeSignup(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (userRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용중인 닉네임입니다.");
        }
        
        user.updateProfile(nickname, user.getProfileImageUrl());
        userRepository.save(user);
        
        return UserInfoResponse.from(user);
    }
    
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        return UserInfoResponse.from(user);
    }
    
    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
        
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        
        // Redis에 저장된 리프레시 토큰과 일치하는지 확인
        String storedToken = tokenService.getRefreshToken(userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        // 새로운 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
        
        // Redis에 새 리프레시 토큰 저장
        tokenService.saveRefreshToken(userId, newRefreshToken, jwtTokenProvider.getRefreshTokenExpiration());
        
        log.info("Token refreshed for user: {}", user.getEmail());
        
        return TokenResponse.of(newAccessToken, newRefreshToken, 86400000L);
    }
    
    @Transactional
    public void logout(Long userId, String accessToken) {
        // 리프레시 토큰 삭제
        tokenService.deleteRefreshToken(userId);
        
        // 액세스 토큰을 블랙리스트에 추가 (남은 유효시간만큼)
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            long remainingTime = jwtTokenProvider.getRemainingExpiration(accessToken);
            if (remainingTime > 0) {
                tokenService.addToBlacklist(accessToken, remainingTime);
            }
        }
        
        log.info("User {} logged out successfully", userId);
    }
}