package com.goormi.routine.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormi.routine.domain.auth.annotation.CurrentUser;
import com.goormi.routine.domain.user.dto.UserRequest;
import com.goormi.routine.domain.user.dto.UserResponse;
import com.goormi.routine.domain.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ResponseEntity<UserResponse.Profile> getMyProfile(@CurrentUser Long userId) {
		UserResponse.Profile profile = userService.getMyProfile(userId);
		return ResponseEntity.ok(profile);
	}

	@PutMapping("/me/profile")
	public ResponseEntity<UserResponse.Profile> updateProfile(
		@CurrentUser Long userId,
		@Valid @RequestBody UserRequest.UpdateProfile request) {

		UserResponse.Profile profile = userService.updateProfile(userId, request);
		return ResponseEntity.ok(profile);
	}

	@PutMapping("/me/settings")
	public ResponseEntity<UserResponse.Profile> updateSettings(
		@CurrentUser Long userId,
		@Valid @RequestBody UserRequest.UpdateSettings request) {

		UserResponse.Profile profile = userService.updateSettings(userId, request);
		return ResponseEntity.ok(profile);
	}

	@GetMapping("/{userId}")
	public ResponseEntity<UserResponse.PublicProfile> getPublicProfile(
		@PathVariable Long userId) {

		UserResponse.PublicProfile profile = userService.getPublicProfile(userId);
		return ResponseEntity.ok(profile);
	}
}
