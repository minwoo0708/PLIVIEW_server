package com.musicreview.server.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Spotify Web API GET 호출. 응답 JSON을 가공하지 않고 바이트 그대로 돌려줍니다.
 * 경로 변수(id)는 호출 전에 반드시 검증된 값이어야 합니다.
 *
 * <p>모든 사용자가 하나의 앱 토큰을 같이 쓰므로, 같은 URL 응답을 잠깐(기본 2분) 캐시해서
 * Spotify 호출 수를 줄이고 429(호출 한도 초과)가 전체 사용자에게 번지는 것을 막습니다.
 */
@Slf4j
@Component
public class SpotifyClient {

    private final SpotifyProperties properties;
    private final SpotifyTokenProvider tokenProvider;
    private final RestClient restClient;
    private final Cache<URI, byte[]> responseCache;

    public SpotifyClient(SpotifyProperties properties, SpotifyTokenProvider tokenProvider, RestClient spotifyRestClient) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.restClient = spotifyRestClient;
        this.responseCache = Caffeine.newBuilder()
                .expireAfterWrite(properties.cacheTtl())
                .maximumSize(properties.cacheMaxEntries())
                .build();
    }

    /**
     * @param pathTemplate  예: "/tracks/{id}"
     * @param pathVariables 템플릿 변수 (검증된 값)
     * @param query         쿼리 파라미터. 값은 URI 변수로 넘겨 엄격하게 인코딩됨 ("C++"의 +도 %2B로)
     */
    public byte[] get(String pathTemplate, Map<String, String> pathVariables, Map<String, String> query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.apiBaseUrl()).path(pathTemplate);
        Map<String, Object> variables = new HashMap<>(pathVariables);
        query.forEach((name, value) -> {
            builder.queryParam(name, "{q_" + name + "}");
            variables.put("q_" + name, value);
        });
        URI uri = builder.encode().buildAndExpand(variables).toUri();

        byte[] cached = responseCache.getIfPresent(uri);
        if (cached != null) {
            return cached;
        }
        byte[] body = fetch(uri);
        responseCache.put(uri, body);
        return body;
    }

    private byte[] fetch(URI uri) {
        String token = tokenProvider.getToken();
        try {
            return call(uri, token);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                // 앱 토큰이 Spotify 쪽에서 먼저 만료된 경우: 이 요청에 쓴 토큰만 버리고 한 번 재시도
                tokenProvider.invalidate(token);
                try {
                    return call(uri, tokenProvider.getToken());
                } catch (RestClientResponseException retry) {
                    throw translate(retry, uri);
                }
            }
            throw translate(e, uri);
        } catch (ResourceAccessException e) {
            log.warn("Spotify 연결 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
        }
    }

    /** 사용자 Spotify 계정 정보 (연동 확인용) */
    public record SpotifyUserProfile(String id, @JsonProperty("display_name") String displayName) {
    }

    /**
     * 사용자가 앱에서 PKCE로 받은 Spotify 토큰으로 GET /v1/me 를 호출해 계정을 확인합니다.
     * 서버는 이 토큰을 저장하지 않고, 응답도 캐시하지 않습니다.
     */
    public SpotifyUserProfile getCurrentUser(String userAccessToken) {
        URI uri = UriComponentsBuilder.fromUriString(properties.apiBaseUrl()).path("/me").build().toUri();
        try {
            SpotifyUserProfile profile = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                    .retrieve()
                    .body(SpotifyUserProfile.class);
            if (profile == null || profile.id() == null) {
                throw new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
            }
            return profile;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 401) {
                throw new BusinessException(ErrorCode.SPOTIFY_TOKEN_INVALID);
            }
            if (status == 403) {
                // 개발 모드에서 Dashboard > User Management에 등록되지 않은 계정
                throw new BusinessException(ErrorCode.SPOTIFY_TOKEN_INVALID,
                        "이 Spotify 계정은 아직 연동할 수 없습니다. 개발자 대시보드의 사용자 목록에 등록돼 있어야 합니다.");
            }
            throw translate(e, uri);
        } catch (ResourceAccessException e) {
            log.warn("Spotify 연결 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
        }
    }

    private byte[] call(URI uri, String token) {
        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(byte[].class);
    }

    /**
     * Spotify 오류 → 우리 에러 코드.
     * 400은 "없는 id"(Invalid base62 id)일 때만 404로 보고, 그 밖의 400은 서버 쪽 요청 실수(market 설정 등)라
     * 로그를 남기고 502로 돌려줍니다. 그래야 설정 실수가 "없는 항목"처럼 숨지 않습니다.
     */
    private BusinessException translate(RestClientResponseException e, URI uri) {
        int status = e.getStatusCode().value();
        String body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
        if (status == 404) {
            return new BusinessException(ErrorCode.SPOTIFY_NOT_FOUND);
        }
        if (status == 400) {
            // Spotify가 없는 id에 주는 문구: "Invalid base62 id", "invalid id"
            String lower = body.toLowerCase(Locale.ROOT);
            if (lower.contains("base62") || lower.matches("(?s).*invalid\\s+id\\b.*")) {
                return new BusinessException(ErrorCode.SPOTIFY_NOT_FOUND);
            }
            log.error("Spotify가 400을 돌려줌 (서버 요청 확인 필요): {} {} → {}", uri.getPath(), uri.getQuery(), body);
            return new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
        }
        if (status == 403) {
            log.warn("Spotify 접근 거부: {} → {}", uri.getPath(), body);
            return new BusinessException(ErrorCode.SPOTIFY_FORBIDDEN);
        }
        if (status == 429) {
            return new BusinessException(ErrorCode.SPOTIFY_RATE_LIMITED);
        }
        log.warn("Spotify 오류 응답: {} {}", status, uri.getPath());
        return new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
    }
}
