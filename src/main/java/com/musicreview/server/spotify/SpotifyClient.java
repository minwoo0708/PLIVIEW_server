package com.musicreview.server.spotify;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Spotify Web API GET 호출. 응답 JSON을 가공하지 않고 바이트 그대로 돌려줍니다.
 * 경로 변수(id)는 호출 전에 반드시 검증된 값이어야 합니다.
 */
@Slf4j
@Component
public class SpotifyClient {

    private final SpotifyProperties properties;
    private final SpotifyTokenProvider tokenProvider;
    private final RestClient restClient;

    public SpotifyClient(SpotifyProperties properties, SpotifyTokenProvider tokenProvider, RestClient spotifyRestClient) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.restClient = spotifyRestClient;
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
        try {
            return call(uri);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()) {
                // 캐시한 앱 토큰이 Spotify 쪽에서 만료된 경우: 새 토큰으로 한 번만 재시도
                tokenProvider.invalidate();
                try {
                    return call(uri);
                } catch (RestClientResponseException retry) {
                    throw translate(retry);
                }
            }
            throw translate(e);
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
     * 서버는 이 토큰을 저장하지 않습니다.
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
            throw translate(e);
        } catch (ResourceAccessException e) {
            log.warn("Spotify 연결 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
        }
    }

    private byte[] call(URI uri) {
        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getToken())
                .retrieve()
                .body(byte[].class);
    }

    private BusinessException translate(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        // 400: 형식은 맞지만 Spotify에 없는 id(Invalid base62 id), 403/404: 비공개·없는 항목
        if (status == 400 || status == 403 || status == 404) {
            return new BusinessException(ErrorCode.SPOTIFY_NOT_FOUND);
        }
        if (status == 429) {
            return new BusinessException(ErrorCode.SPOTIFY_RATE_LIMITED);
        }
        log.warn("Spotify 오류 응답: {}", status);
        return new BusinessException(ErrorCode.SPOTIFY_UNAVAILABLE);
    }
}
