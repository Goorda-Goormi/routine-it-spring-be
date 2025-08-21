package com.goormi.routine.domain.auth.service;

import com.goormi.routine.domain.auth.dto.AuthResponse;

public interface SimpleAuthService {

	/**
	 * 카카오 로그인 프로세스를 처리하고, 토큰 및 사용자 정보를 반환합니다.
	 * @param code 카카오 인가 코드
	 * @return 인증 응답 (토큰, 사용자 정보, 신규 가입 여부)
	 */
	AuthResponse processKakaoLogin(String code);
}