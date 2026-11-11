package com.musicreview.server.spotify;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.review.domain.ItemType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 리뷰 대상의 진짜 정보(이름, 아티스트·주인, 커버, 플리 주인 id)를 Spotify에서 확인합니다.
 * 앱이 보낸 표시용 정보를 그대로 믿으면 아무 이름이나 저장되고, 플리 주인 id를 속여 큐레이터 랭킹을
 * 조작할 수 있기 때문입니다.
 *
 * <ul>
 *   <li>Spotify에 없는 id → 404 SPOTIFY_NOT_FOUND (리뷰를 만들지 않음)</li>
 *   <li>Spotify 설정이 없거나 일시 장애·호출 한도 → {@link Optional#empty()} (앱이 보낸 값으로 저장)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpotifyItemLookup {

    public record SpotifyItem(String name, String subtitle, String imageUrl, String ownerId, String snapshotId) {
    }

    private final SpotifyClient spotifyClient;
    private final SpotifyProperties properties;
    private final JsonMapper jsonMapper;

    public Optional<SpotifyItem> lookup(ItemType type, String spotifyId) {
        if (!properties.isConfigured()) {
            return Optional.empty();
        }
        String path = switch (type) {
            case TRACK -> "/tracks/{id}";
            case ALBUM -> "/albums/{id}";
            case PLAYLIST -> "/playlists/{id}";
        };
        Map<String, String> query = properties.market() == null || properties.market().isBlank()
                ? Map.of()
                : Map.of("market", properties.market());
        byte[] body;
        try {
            body = spotifyClient.get(path, Map.of("id", spotifyId), query);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.SPOTIFY_NOT_FOUND) {
                throw e;
            }
            log.warn("Spotify 확인 실패({}), 앱이 보낸 표시 정보로 저장: {} {}", e.getErrorCode(), type, spotifyId);
            return Optional.empty();
        }
        return Optional.of(parse(type, jsonMapper.readTree(body)));
    }

    private static SpotifyItem parse(ItemType type, JsonNode node) {
        String name = text(node.path("name"));
        return switch (type) {
            case TRACK -> new SpotifyItem(name, artistNames(node.path("artists")),
                    firstImage(node.path("album").path("images")), null, null);
            case ALBUM -> new SpotifyItem(name, artistNames(node.path("artists")),
                    firstImage(node.path("images")), null, null);
            case PLAYLIST -> {
                JsonNode owner = node.path("owner");
                String ownerId = text(owner.path("id"));
                String ownerName = text(owner.path("display_name"));
                yield new SpotifyItem(name, ownerName != null ? ownerName : ownerId,
                        firstImage(node.path("images")), ownerId, text(node.path("snapshot_id")));
            }
        };
    }

    private static String artistNames(JsonNode artists) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < artists.size(); i++) {
            String name = text(artists.get(i).path("name"));
            if (name != null) {
                names.add(name);
            }
        }
        return names.isEmpty() ? null : String.join(", ", names);
    }

    private static String firstImage(JsonNode images) {
        return images.isArray() && images.size() > 0 ? text(images.get(0).path("url")) : null;
    }

    private static String text(JsonNode node) {
        String value = node.stringValue(null);
        return value == null || value.isBlank() ? null : value;
    }
}
