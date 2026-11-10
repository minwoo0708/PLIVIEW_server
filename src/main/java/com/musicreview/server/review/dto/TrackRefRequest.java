package com.musicreview.server.review.dto;

import com.musicreview.server.global.validation.SpotifyId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 앨범·플리 리뷰에서 고른 곡 (최고의 곡, 워스트 곡, 과소평가된 곡) */
public record TrackRefRequest(
        @NotNull(message = "곡 spotifyId를 입력해 주세요.")
        @SpotifyId
        String spotifyId,

        @NotBlank(message = "곡 이름을 입력해 주세요.")
        @Size(max = 200, message = "곡 이름은 200자 이하여야 합니다.")
        String name
) {
}
