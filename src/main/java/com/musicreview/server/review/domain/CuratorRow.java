package com.musicreview.server.review.domain;

/** 큐레이터(Spotify를 연동한 사용자)별 플리 집계 결과 */
public record CuratorRow(
        Long userId,
        String ownerSpotifyId,
        Long playlistCount,
        Long reviewCount,
        Double averageRating,
        Double averageConcept,
        Double averageFlow,
        Double averageDiscovery
) {
}
