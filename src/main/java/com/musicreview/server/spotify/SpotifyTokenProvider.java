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
 *
 * <p>유효한 토큰이 있으면 잠금 없이 바로 돌려줍니다. 갱신이 필요할 때만 한 스레드가 토큰 서버를 호출하고,
 * 그동안 갱신을 기다리는 스레드만 대기합니다.
 */
@Slf4j
@Component
public class SpotifyTokenProvider {

    private static final long REFRESH_MARGIN_SECONDS = 60;

    private final SpotifyProperties properties;
    private final RestClient restClient;
    private final Object refreshLock = new Object();

    /** 토큰과 만료 시각을 한 번에 바꿔서 읽는 쪽이 어긋난 조합을 보지 않게 함 */
    private record CachedToken(String value, Instant expiresAt) {
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    private volatile CachedToken current;

    public SpotifyTokenProvider(SpotifyProperties properties, RestClient spotifyRestClient) {
        this.properties = properties;
        this.restClient = spotifyRestClient;
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("expires_in") long expiresIn) {
    }

    public String getToken() {
        CachedToken token = current;
        if (token != null && token.isValid()) {
            return token.value();
        }
        synchronized (refreshLock) {
            token = current;
            if (token != null && token.isValid()) {
                return token.value(); // 기다리는 동안 다른 스레드가 이미 갱신함
            }
            TokenResponse response = requestToken();
            current = new CachedToken(response.accessToken(),
                    Instant.now().plusSeconds(Math.max(0, response.expiresIn() - REFRESH_MARGIN_SECONDS)));
            return current.value();
        }
    }

    /**
     * Spotify가 401을 줬을 때 그 요청에 쓴 토큰만 버립니다.
     * 다른 스레드가 이미 새 토큰으로 바꿔 놓았다면 새 토큰은 건드리지 않습니다.
     */
    public void invalidate(String staleToken) {
        synchronized (refreshLock) {
            CachedToken token = current;
            if (token != null && token.value().equals(staleToken)) {
                current = null;
            }
        }
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
