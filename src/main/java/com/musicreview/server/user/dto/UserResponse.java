package com.musicreview.server.user.dto;

import com.musicreview.server.global.common.TimeUtils;
import com.musicreview.server.user.domain.User;

import java.time.OffsetDateTime;

/** 본인 정보 응답 (이메일 포함). 다른 사람에게는 review.dto.AuthorResponse를 씀 */
public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        OffsetDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                TimeUtils.toOffset(user.getCreatedAt())
        );
    }
}
