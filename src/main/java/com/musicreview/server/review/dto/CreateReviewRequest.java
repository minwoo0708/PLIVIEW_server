package com.musicreview.server.review.dto;

import com.musicreview.server.global.validation.Score;
import com.musicreview.server.global.validation.SpotifyId;
import com.musicreview.server.review.domain.ItemType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/** 타입별 필수/금지 필드(플리 전용 항목)는 ReviewService에서 검사 */
public record CreateReviewRequest(
        @NotNull(message = "type을 입력해 주세요.")
        ItemType type,

        @NotNull(message = "spotifyId를 입력해 주세요.")
        @SpotifyId
        String spotifyId,

        @NotNull(message = "item을 입력해 주세요.")
        @Valid
        ItemInfoRequest item,

        @NotNull(message = "별점을 입력해 주세요.")
        @Score
        BigDecimal rating,

        @Size(max = 200, message = "한 줄 평은 200자 이하여야 합니다.")
        String comment,

        @Size(max = 5, message = "태그는 5개까지 달 수 있습니다.")
        List<@NotBlank(message = "빈 태그는 달 수 없습니다.") @Size(max = 20, message = "태그는 20자 이하여야 합니다.") String> tags,

        @Score
        BigDecimal conceptScore,

        @Score
        BigDecimal flowScore,

        @Score
        BigDecimal discoveryScore,

        @Size(max = 100, message = "snapshotId는 100자 이하여야 합니다.")
        String snapshotId,

        @Size(max = 64, message = "ownerSpotifyId는 64자 이하여야 합니다.")
        String ownerSpotifyId
) {
}
