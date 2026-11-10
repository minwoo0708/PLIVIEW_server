package com.musicreview.server.curator.dto;

import java.math.BigDecimal;

/** docs/API.md 22. 큐레이터(플리 주인) 랭킹 한 줄 */
public record CuratorRankingResponse(
        int rank,
        String ownerSpotifyId,
        String ownerName,
        long playlistCount,
        long reviewCount,
        BigDecimal averageRating,
        AverageScores averageScores
) {

    public record AverageScores(BigDecimal concept, BigDecimal flow, BigDecimal discovery) {
    }
}
