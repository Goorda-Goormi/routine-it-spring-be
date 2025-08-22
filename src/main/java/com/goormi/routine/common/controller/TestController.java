package com.goormi.routine.common.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormi.routine.common.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Component
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

	private final JwtUtil jwtUtil;

	@PostMapping("/token")
	public ResponseEntity<Map<String, String>> generateTestToken() {
		String accessToken = jwtUtil.generateAccessToken(1L, "testUser");
		String refreshToken = jwtUtil.generateRefreshToken(1L);

		Map<String, String> tokens = new HashMap<>();
		tokens.put("accessToken", accessToken);
		tokens.put("refreshToken", refreshToken);

		return ResponseEntity.ok(tokens);
	}

	@GetMapping("/protected")
	public ResponseEntity<String> testProtected() {
		return ResponseEntity.ok("JWT 인증 성공!");
	}
}