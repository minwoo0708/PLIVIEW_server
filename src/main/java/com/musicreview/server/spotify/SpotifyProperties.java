package com.musicreview.server.spotify;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.spotify")
public record SpotifyProperties(
        String clientId,
        String clientSecret,
        String apiBaseUrl,
        String tokenUrl,
        String market,
        Duration connectTimeout,
        Duration readTimeout,
        /** 같은 Spotify 요청 응답을 재사용하는 시간 */
        Duration cacheTtl,
        long cacheMaxEntries,
        /** 사용자 한 명이 1분 동안 보낼 수 있는 /api/spotify/** 요청 수 */
        int requestsPerMinutePerUser
) {

    public SpotifyProperties {
        cacheTtl = cacheTtl == null ? Duration.ofMinutes(2) : cacheTtl;
        cacheMaxEntries = cacheMaxEntries <= 0 ? 2000 : cacheMaxEntries;
        requestsPerMinutePerUser = requestsPerMinutePerUser <= 0 ? 60 : requestsPerMinutePerUser;
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
