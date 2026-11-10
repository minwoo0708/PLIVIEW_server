package com.musicreview.server.curator.dto;

import java.math.BigDecimal;

/** docs/API.md 22. 베스트 큐레이터 랭킹 한 줄 (Spotify를 연동한 사용자만) */
public record CuratorRankingResponse(
        int rank,
        Long userId,
        String nickname,
        String profileImageUrl,
        String spotifyUserId,
        String spotifyDisplayName,
        long playlistCount,
        long reviewCount,
        BigDecimal averageRating,
        AverageScores averageScores
) {

    public record AverageScores(BigDecimal concept, BigDecimal flow, BigDecimal discovery) {
    }
}
