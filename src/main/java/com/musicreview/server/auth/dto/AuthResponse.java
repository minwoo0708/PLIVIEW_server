package com.musicreview.server.auth.dto;

import com.musicreview.server.global.security.JwtProvider;
import com.musicreview.server.user.domain.User;
import com.musicreview.server.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {

    public static AuthResponse of(JwtProvider.IssuedToken token, User user) {
        return new AuthResponse(token.value(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }
}
