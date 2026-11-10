package com.musicreview.server.user.dto;

import jakarta.validation.constraints.NotBlank;

/** 앱이 PKCE로 받은 사용자 Spotify Access Token. 서버는 확인만 하고 저장하지 않음 */
public record LinkSpotifyRequest(
        @NotBlank(message = "spotifyAccessToken을 입력해 주세요.")
        String spotifyAccessToken
) {
}
