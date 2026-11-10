package com.musicreview.server.user.dto;

import com.musicreview.server.global.common.TimeUtils;
import com.musicreview.server.user.domain.User;

import java.time.OffsetDateTime;

/** 본인 정보 응답 (이메일 포함). 다른 사람에게는 이메일 없는 응답을 씀 */
public record UserResponse(
        Long id,
        String email,
        String nickname,
        String profileImageUrl,
        SpotifyAccount spotify,
        OffsetDateTime createdAt
) {

    /** 연동한 Spotify 계정. 연동 안 했으면 null */
    public record SpotifyAccount(String userId, String displayName) {

        public static SpotifyAccount from(User user) {
            return user.isSpotifyLinked()
                    ? new SpotifyAccount(user.getSpotifyUserId(), user.getSpotifyDisplayName())
                    : null;
        }
    }

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                SpotifyAccount.from(user),
                TimeUtils.toOffset(user.getCreatedAt())
        );
    }
}
