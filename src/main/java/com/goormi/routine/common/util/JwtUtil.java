package com.goormi.routine.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@Slf4j
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.access-token-validity}")
	private long accessTokenValidity;

	@Value("${jwt.refresh-token-validity}")
	private long refreshTokenValidity;

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(Long userId, String nickname) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("userId", userId);
		claims.put("nickname", nickname);
		claims.put("type", "access");

		return Jwts.builder()
			.setClaims(claims)
			.setSubject(userId.toString())
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity))
			.signWith(getSigningKey())
			.compact();
	}

	public String generateRefreshToken(Long userId) {
		return Jwts.builder()
			.setSubject(userId.toString())
			.claim("type", "refresh")
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + refreshTokenValidity))
			.signWith(getSigningKey())
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.setSigningKey(getSigningKey())
				.parseClaimsJws(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("Invalid JWT token: {}", e.getMessage());
			return false;
		}
	}

	public Long getUserIdFromToken(String token) {
		Claims claims = Jwts.parser()
			.setSigningKey(getSigningKey())
			.parseClaimsJws(token)
			.getBody();
		return Long.parseLong(claims.getSubject());
	}

	public long getTokenRemainingTime(String token) {
		try {
			Claims claims = Jwts.parser()
				.setSigningKey(getSigningKey())
				.parseClaimsJws(token)
				.getBody();

			Date expiration = claims.getExpiration();
			Date now = new Date();

			return Math.max(0, expiration.getTime() - now.getTime());
		} catch (Exception e) {
			log.warn("Failed to get token remaining time: {}", e.getMessage());
			return 0;
		}
	}

}