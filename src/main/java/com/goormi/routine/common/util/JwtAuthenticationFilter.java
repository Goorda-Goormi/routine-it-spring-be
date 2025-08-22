package com.goormi.routine.common.util;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.goormi.routine.domain.auth.service.TokenService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final TokenService tokenService;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain) throws ServletException, IOException {

		String token = extractToken(request);

		if (token != null && jwtUtil.validateToken(token) && !tokenService.isBlacklisted(token)) {
			try {
				Long userId = jwtUtil.getUserIdFromToken(token);

				UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (Exception e) {
				log.warn("JWT authentication failed: {}", e.getMessage());
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}

	private String extractToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/auth/") ||
			path.startsWith("/swagger-ui/") ||
			path.startsWith("/v3/api-docs/");
	}
}