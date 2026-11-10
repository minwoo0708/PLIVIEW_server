package com.musicreview.server.item.dto;

import java.math.BigDecimal;

/** 앨범·플리 수록곡 목록에 표시할 곡별 평점. 리뷰가 없으면 count 0, 평균 null */
public record TrackRatingResponse(
        String spotifyId,
        long reviewCount,
        BigDecimal averageRating,
        BigDecimal myRating
) {
}
