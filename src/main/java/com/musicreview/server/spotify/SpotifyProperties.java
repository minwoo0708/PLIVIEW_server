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
        Duration readTimeout
) {

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
