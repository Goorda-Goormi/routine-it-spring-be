package com.goormi.routine.domain.auth.service;

import com.goormi.routine.domain.user.entity.User;
import com.goormi.routine.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        log.info("OAuth2 User Info: {}", oAuth2User.getAttributes());
        
        // 카카오 사용자 정보 파싱
        Map<String, Object> attributes = oAuth2User.getAttributes();
        // 카카오 ID는 Long 타입으로 반환됨
        Object idAttribute = attributes.get("id");
        log.info("Kakao ID attribute type: {}, value: {}", idAttribute != null ? idAttribute.getClass().getName() : "null", idAttribute);
        
        String kakaoId;
        if (idAttribute instanceof Long) {
            kakaoId = String.valueOf(idAttribute);
        } else if (idAttribute instanceof String) {
            kakaoId = (String) idAttribute;
        } else {
            log.error("Unexpected Kakao ID type: {}", idAttribute != null ? idAttribute.getClass().getName() : "null");
            throw new IllegalStateException("카카오 ID 타입이 예상과 다릅니다: " + (idAttribute != null ? idAttribute.getClass().getName() : "null"));
        }
        
        // kakao_account 정보 추출 (선택적)
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = null;
        String nickname = null;
        String profileImageUrl = null;
        
        if (kakaoAccount != null) {
            // 이메일 추출
            email = (String) kakaoAccount.get("email");
            
            // 프로필 정보 추출
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                nickname = (String) profile.get("nickname");
                profileImageUrl = (String) profile.get("profile_image_url");
            }
        }
        
        // 기본값 설정
        if (email == null || email.isEmpty()) {
            email = kakaoId + "@kakao.com";
        }
        if (nickname == null || nickname.isEmpty()) {
            nickname = "사용자" + kakaoId.substring(0, Math.min(kakaoId.length(), 6));
        }
        
        // 사용자 조회 또는 생성
        final String finalEmail = email;
        final String finalNickname = nickname;
        final String finalProfileImageUrl = profileImageUrl;
        
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> {
                    User newUser = User.createKakaoUser(kakaoId, finalEmail, finalNickname, finalProfileImageUrl);
                    log.info("Creating new user with Kakao ID: {}, email: {}, nickname: {}", kakaoId, finalEmail, finalNickname);
                    return userRepository.save(newUser);
                });
        
        log.info("User {} logged in successfully", user.getEmail());
        
        // OAuth2User 객체 반환 (userId를 name으로 사용)
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "id"
        );
    }
}