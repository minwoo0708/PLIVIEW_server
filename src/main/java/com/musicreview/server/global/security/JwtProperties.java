package com.musicreview.server.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {
}
