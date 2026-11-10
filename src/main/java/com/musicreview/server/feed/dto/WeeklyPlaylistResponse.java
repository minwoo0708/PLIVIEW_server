package com.musicreview.server.feed.dto;

import com.musicreview.server.review.dto.ReviewResponse;

import java.math.BigDecimal;

/** 홈 "이번 주의 플리" 한 줄 */
public record WeeklyPlaylistResponse(
        int rank,
        String spotifyId,
        ReviewResponse.Item item,
        long reviewCount,
        BigDecimal averageRating
) {
}
