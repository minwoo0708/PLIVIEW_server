package com.musicreview.server.curator.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 큐레이터 프로필 상단: 사용자 정보 + 그 사람 플리가 받은 평가 요약 */
public record CuratorProfileResponse(
        Long userId,
        String nickname,
        String profileImageUrl,
        String spotifyUserId,
        String spotifyDisplayName,
        long playlistCount,
        long reviewCount,
        BigDecimal averageRating,
        CuratorRankingResponse.AverageScores averageScores,
        OffsetDateTime createdAt
) {
}
