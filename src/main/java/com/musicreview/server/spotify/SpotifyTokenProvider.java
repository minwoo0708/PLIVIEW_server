package com.musicreview.server.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Client Credentials 방식으로 받은 앱 토큰을 만료 1분 전까지 재사용합니다.
 * (사용자 Spotify 계정과 무관: 검색·곡/앨범/공개 플리 조회 전용)
 */
@Slf4j
@Component
public class SpotifyTokenProvider {

    private static final long REFRESH_MARGIN_SECONDS = 60;

    private final SpotifyProperties properties;
    private final RestClient restClient;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    public SpotifyTokenProvider(SpotifyProperties properties, RestClient spotifyRestClient) {
        this.properties = properties;
        this.restClient = spotifyRestClient;
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }

    public synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
            return cachedToken;
        }
        TokenResponse response = requestToken();
        cachedToken = response.accessToken();
        expiresAt = Instant.now().plusSeconds(Math.max(0, response.expiresIn() - REFRESH_MARGIN_SECONDS));
        return cachedToken;
    }

    /** Spotify가 401을 주면 토큰을 버리고 다시 받도록 */
    public synchronized void invalidate() {
        cachedToken = null;
        expiresAt = Instant.EPOCH;
    }

    private TokenResponse requestToken() {
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.SPOTIFY_NOT_CONFIGURED);
        }
        String credentials = Base64.getEncoder().encodeToString(
                (properties.clientId() + ":" + properties.clientSecret()).getBytes(StandardCharsets.UTF_8));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        try {
            TokenResponse response = restClient.post()
                    .uri(properties.tokenUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (response == null || response.accessToken() == null) {
                throw new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
            }
            return response;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().is4xxClientError()) {
                log.error("Spotify 토큰 발급 실패({}): SPOTIFY_CLIENT_ID / SPOTIFY_CLIENT_SECRET을 확인하세요.", e.getStatusCode());
                throw new BusinessException(ErrorCode.SPOTIFY_NOT_CONFIGURED);
            }
            throw new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
        } catch (ResourceAccessException e) {
            log.warn("Spotify 토큰 서버 연결 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
        }
    }
}
