package com.musicreview.server.review.domain;

/** 플리 주인(큐레이터)별 집계 결과 */
public record CuratorRow(
        String ownerSpotifyId,
        Long playlistCount,
        Long reviewCount,
        Double averageRating,
        Double averageConcept,
        Double averageFlow,
        Double averageDiscovery
) {
}
