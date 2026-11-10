package com.musicreview.server.review.domain;

/** JPQL 집계 결과. 평균은 리뷰가 없으면 null */
public record ItemStats(
        Long reviewCount,
        Double averageRating,
        Double averageConcept,
        Double averageFlow,
        Double averageDiscovery
) {
}
