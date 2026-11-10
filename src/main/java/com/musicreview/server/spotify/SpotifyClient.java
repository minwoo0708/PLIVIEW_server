package com.musicreview.server.spotify;

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
