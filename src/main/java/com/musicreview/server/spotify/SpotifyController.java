package com.musicreview.server.spotify;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
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

    @GetMapping("/search")
    public ResponseEntity<byte[]> search(
            @RequestParam @Size(min = 1, max = 100, message = "검색어는 1~100자입니다.") String q,
            @RequestParam String type,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit은 1~50입니다.")
            @Max(value = 50, message = "limit은 1~50입니다.") int limit,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "offset은 0~1000입니다.")
            @Max(value = 1000, message = "offset은 0~1000입니다.") int offset) {
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
    public ResponseEntity<byte[]> track(@PathVariable @SpotifyId String id) {
        return json(spotifyClient.get("/tracks/{id}", Map.of("id", id), withMarket()));
    }

    @GetMapping("/albums/{id}")
    public ResponseEntity<byte[]> album(@PathVariable @SpotifyId String id) {
        return json(spotifyClient.get("/albums/{id}", Map.of("id", id), withMarket()));
    }

    @GetMapping("/artists/{id}/albums")
    public ResponseEntity<byte[]> artistAlbums(
            @PathVariable @SpotifyId String id,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit은 1~50입니다.")
            @Max(value = 50, message = "limit은 1~50입니다.") int limit,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "offset은 0 이상입니다.") int offset) {
        Map<String, String> query = withMarket();
        query.put("limit", String.valueOf(limit));
        query.put("offset", String.valueOf(offset));
        return json(spotifyClient.get("/artists/{id}/albums", Map.of("id", id), query));
    }

    @GetMapping("/playlists/{id}")
    public ResponseEntity<byte[]> playlist(@PathVariable @SpotifyId String id) {
        return json(spotifyClient.get("/playlists/{id}", Map.of("id", id), withMarket()));
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
