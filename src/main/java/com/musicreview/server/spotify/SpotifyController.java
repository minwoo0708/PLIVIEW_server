package com.musicreview.server.spotify;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.global.security.CurrentUserId;
import com.musicreview.server.global.validation.SpotifyId;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 앱이 Spotify 로그인 없이 음악 정보를 볼 수 있도록 서버가 대신 조회합니다 (docs/API.md 7~11).
 * 응답은 Spotify Web API JSON 그대로입니다.
 */
@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private static final Set<String> SEARCH_TYPES = Set.of("track", "album", "artist", "playlist");

    private final SpotifyClient spotifyClient;
    private final SpotifyProperties properties;
    private final SpotifyRateLimiter rateLimiter;

    @GetMapping("/search")
    public ResponseEntity<byte[]> search(
            @CurrentUserId Long userId,
            @RequestParam @Size(min = 1, max = 100, message = "검색어는 1~100자입니다.") String q,
            @RequestParam String type,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit은 1~50입니다.")
            @Max(value = 50, message = "limit은 1~50입니다.") int limit,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "offset은 0~1000입니다.")
            @Max(value = 1000, message = "offset은 0~1000입니다.") int offset) {
        rateLimiter.check(userId);
        String searchType = type.strip().toLowerCase(Locale.ROOT);
        if (!SEARCH_TYPES.contains(searchType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "type은 track, album, artist, playlist 중 하나여야 합니다.");
        }
        if (q.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "검색어를 입력해 주세요.");
        }
        Map<String, String> query = withMarket();
        query.put("q", q.strip());
        query.put("type", searchType);
        query.put("limit", String.valueOf(limit));
        query.put("offset", String.valueOf(offset));
        return json(spotifyClient.get("/search", Map.of(), query));
    }

    @GetMapping("/tracks/{id}")
    public ResponseEntity<byte[]> track(@CurrentUserId Long userId, @PathVariable @SpotifyId String id) {
        rateLimiter.check(userId);
        return json(spotifyClient.get("/tracks/{id}", Map.of("id", id), withMarket()));
    }

    @GetMapping("/albums/{id}")
    public ResponseEntity<byte[]> album(@CurrentUserId Long userId, @PathVariable @SpotifyId String id) {
        rateLimiter.check(userId);
        return json(spotifyClient.get("/albums/{id}", Map.of("id", id), withMarket()));
    }

    @GetMapping("/artists/{id}/albums")
    public ResponseEntity<byte[]> artistAlbums(
            @CurrentUserId Long userId,
            @PathVariable @SpotifyId String id,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit은 1~50입니다.")
            @Max(value = 50, message = "limit은 1~50입니다.") int limit,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "offset은 0 이상입니다.") int offset) {
        rateLimiter.check(userId);
        Map<String, String> query = withMarket();
        query.put("limit", String.valueOf(limit));
        query.put("offset", String.valueOf(offset));
        return json(spotifyClient.get("/artists/{id}/albums", Map.of("id", id), query));
    }

    /** 플리 정보. Spotify가 이 응답에 수록곡을 더 이상 넣어 주지 않으므로 수록곡은 /tracks를 따로 호출 */
    @GetMapping("/playlists/{id}")
    public ResponseEntity<byte[]> playlist(@CurrentUserId Long userId, @PathVariable @SpotifyId String id) {
        rateLimiter.check(userId);
        return json(spotifyClient.get("/playlists/{id}", Map.of("id", id), withMarket()));
    }

    /**
     * 플리 수록곡 (페이지). Spotify의 새 경로(/items)를 먼저 쓰고, 없으면 예전 경로(/tracks)로 다시 시도.
     * 응답은 Spotify 페이지 JSON 그대로: { items: [ { track 또는 item: {...} } ], total, next, ... }
     */
    @GetMapping("/playlists/{id}/tracks")
    public ResponseEntity<byte[]> playlistTracks(
            @CurrentUserId Long userId,
            @PathVariable @SpotifyId String id,
            @RequestParam(defaultValue = "50") @Min(value = 1, message = "limit은 1~50입니다.")
            @Max(value = 50, message = "limit은 1~50입니다.") int limit,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "offset은 0 이상입니다.") int offset) {
        rateLimiter.check(userId);
        Map<String, String> query = withMarket();
        query.put("limit", String.valueOf(limit));
        query.put("offset", String.valueOf(offset));
        try {
            return json(spotifyClient.get("/playlists/{id}/items", Map.of("id", id), query));
        } catch (BusinessException e) {
            if (e.getErrorCode() != ErrorCode.SPOTIFY_NOT_FOUND) {
                throw e;
            }
            return json(spotifyClient.get("/playlists/{id}/tracks", Map.of("id", id), query));
        }
    }

    private Map<String, String> withMarket() {
        Map<String, String> query = new LinkedHashMap<>();
        if (properties.market() != null && !properties.market().isBlank()) {
            query.put("market", properties.market());
        }
        return query;
    }

    private static ResponseEntity<byte[]> json(byte[] body) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
