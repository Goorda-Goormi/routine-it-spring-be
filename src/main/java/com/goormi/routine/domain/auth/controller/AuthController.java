package com.goormi.routine.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormi.routine.common.util.JwtUtil;
import com.goormi.routine.domain.auth.annotation.CurrentUser;
import com.goormi.routine.domain.auth.dto.AuthResponse;
import com.goormi.routine.domain.auth.dto.KakaoLoginRequest;
import com.goormi.routine.domain.auth.service.SimpleAuthService;
import com.goormi.routine.domain.auth.service.TokenService;
import com.goormi.routine.domain.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "인증 API", description = "로그인 및 회원 관리")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;
	private final SimpleAuthService simpleAuthService;
	private final TokenService tokenService;
	private final JwtUtil jwtUtil;

	@Operation(summary = "카카오 로그인", description = "카카오 인가 코드를 이용해 로그인하거나 회원가입합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그인 성공",
			content = @Content(schema = @Schema(implementation = AuthResponse.class))),
		@ApiResponse(responseCode = "400", description = "잘못된 요청")
	})
	@PostMapping("/kakao")
	public ResponseEntity<AuthResponse> kakaoLogin(
		@Valid @RequestBody KakaoLoginRequest request) {

		AuthResponse response = simpleAuthService.processKakaoLogin(request.getCode());
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "로그아웃", description = "현재 사용자의 리프레시 토큰을 삭제하고, 액세스 토큰을 블랙리스트에 추가합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "로그아웃 완료"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청 또는 토큰 오류")
	})
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(
		@Parameter(hidden = true) @RequestHeader("Authorization") String authorization,
		@Parameter(hidden = true) @CurrentUser Long userId) {

		String accessToken = authorization.replace("Bearer ", "");
		long remainingTime = System.currentTimeMillis() + jwtUtil.getAccessTokenValidity();
		tokenService.addToBlacklist(accessToken, remainingTime);

		userService.clearRefreshToken(userId);

		return ResponseEntity.ok().build();
	}

	@Operation(summary = "회원 탈퇴", description = "사용자 계정을 비활성화합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "회원 탈퇴 완료"),
		@ApiResponse(responseCode = "400", description = "잘못된 요청")
	})
	@DeleteMapping("/signout")
	public ResponseEntity<Void> signOut(@Parameter(hidden = true) @CurrentUser Long userId) {
		userService.deactivateUser(userId);
		return ResponseEntity.ok().build();
	}
}
