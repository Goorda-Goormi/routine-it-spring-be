package com.goormi.routine.domain.auth.dto;

import com.goormi.routine.domain.user.entity.User;

public record UserInfoResponse(
    Long id,
    String email,
    String nickname,
    String profileImageUrl,
    String role
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
            user.getUserId(),
            user.getEmail(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getRole().name()
        );
    }
}