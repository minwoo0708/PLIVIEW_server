package com.musicreview.server.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Flutter 웹이 다른 포트(origin)에서 API를 부르기 위한 허용 목록 */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOriginPatterns) {

    public CorsProperties {
        allowedOriginPatterns = allowedOriginPatterns == null ? List.of() : List.copyOf(allowedOriginPatterns);
    }
}
