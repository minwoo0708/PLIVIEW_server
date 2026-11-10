package com.musicreview.server.review.dto;

import com.musicreview.server.global.validation.Score;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * PATCH: null이거나 빠진 필드는 그대로 둠.
 * comment ""이면 삭제, tags []이면 모두 삭제. type, spotifyId, ownerSpotifyId는 변경 불가(보내도 무시).
 */
public record UpdateReviewRequest(
        @Valid
        ItemInfoRequest item,

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
        String snapshotId
) {

    public boolean hasPlaylistFields() {
        return conceptScore != null || flowScore != null || discoveryScore != null || snapshotId != null;
    }
}
