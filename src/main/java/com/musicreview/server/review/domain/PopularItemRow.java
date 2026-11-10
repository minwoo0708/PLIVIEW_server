package com.musicreview.server.review.domain;

/** 인기 항목 집계 결과 */
public record PopularItemRow(ItemType type, String spotifyId, Long reviewCount, Double averageRating) {
}
