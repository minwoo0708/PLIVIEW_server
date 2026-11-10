package com.musicreview.server.feed.dto;

import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.dto.ReviewResponse;

import java.math.BigDecimal;

/** docs/API.md 20. 인기 항목. item은 그 항목의 가장 최근 리뷰에 저장된 표시용 정보 */
public record PopularItemResponse(
        ItemType type,
        String spotifyId,
        ReviewResponse.Item item,
        long reviewCount,
        BigDecimal averageRating
) {
}
