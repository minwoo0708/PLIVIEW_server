package com.musicreview.server.item.dto;

import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.dto.ReviewResponse;

import java.math.BigDecimal;
import java.util.Map;

/** docs/API.md 18. 항목 요약 */
public record ItemSummaryResponse(
        ItemType type,
        String spotifyId,
        long reviewCount,
        BigDecimal averageRating,
        Map<String, Long> ratingDistribution,
        PlaylistAverages playlistAverages,
        ReviewResponse myReview
) {

    public record PlaylistAverages(BigDecimal concept, BigDecimal flow, BigDecimal discovery) {
    }
}
