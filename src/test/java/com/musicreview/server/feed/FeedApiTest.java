package com.musicreview.server.feed;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedApiTest extends ApiTestSupport {

    private String review(String type, String spotifyId, String name, double rating) {
        String playlistFields = type.equals("PLAYLIST")
                ? ",\"conceptScore\":4.0,\"flowScore\":4.0,\"discoveryScore\":4.0,\"ownerSpotifyId\":\"owner\""
                : "";
        return """
                {"type":"%s","spotifyId":"%s","item":{"name":"%s","subtitle":"아티스트"},"rating":%s%s}
                """.formatted(type, spotifyId, name, rating, playlistFields);
    }

    @Test
    @DisplayName("피드: 최신순, 타입 필터, 페이지")
    void feed() throws Exception {
        String a = signupAndGetToken("에이");
        String b = signupAndGetToken("비이");
        postJson("/api/reviews", a, review("TRACK", "t1", "첫곡", 4.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, review("ALBUM", "al1", "앨범", 3.0)).andExpect(status().isCreated());
        postJson("/api/reviews", a, review("TRACK", "t2", "둘째곡", 5.0)).andExpect(status().isCreated());

        getJson("/api/feed?size=2", a)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].item.name").value("둘째곡"))
                .andExpect(jsonPath("$.content[0].isMine").value(true))
                .andExpect(jsonPath("$.content[1].item.name").value("앨범"))
                .andExpect(jsonPath("$.content[1].isMine").value(false))
                .andExpect(jsonPath("$.hasNext").value(true));

        getJson("/api/feed?type=album", a)
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("ALBUM"));

        getJson("/api/feed?type=video", a)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ITEM_TYPE"));
    }

    @Test
    @DisplayName("인기 항목: 리뷰 수 내림차순, 같으면 평균 높은 순, 표시 정보는 최근 리뷰 기준")
    void popular() throws Exception {
        String a = signupAndGetToken("에이");
        String b = signupAndGetToken("비이");
        String c = signupAndGetToken("씨이");
        // hot: 리뷰 3개, mid: 2개(평균 5.0), low: 2개(평균 3.0), one: 1개
        postJson("/api/reviews", a, review("TRACK", "hot", "옛 이름", 4.0));
        postJson("/api/reviews", b, review("TRACK", "hot", "옛 이름", 3.0));
        postJson("/api/reviews", c, review("TRACK", "hot", "새 이름", 5.0));
        postJson("/api/reviews", a, review("ALBUM", "mid", "미드", 5.0));
        postJson("/api/reviews", b, review("ALBUM", "mid", "미드", 5.0));
        postJson("/api/reviews", a, review("PLAYLIST", "low", "로우", 3.0));
        postJson("/api/reviews", b, review("PLAYLIST", "low", "로우", 3.0));
        postJson("/api/reviews", c, review("TRACK", "one", "원", 5.0));

        getJson("/api/feed/popular?limit=3", a)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].spotifyId").value("hot"))
                .andExpect(jsonPath("$[0].reviewCount").value(3))
                .andExpect(jsonPath("$[0].averageRating").value(4.0))
                .andExpect(jsonPath("$[0].item.name").value("새 이름"))
                .andExpect(jsonPath("$[1].spotifyId").value("mid"))
                .andExpect(jsonPath("$[2].spotifyId").value("low"));

        getJson("/api/feed/popular?type=TRACK", a)
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].spotifyId").value("one"));

        getJson("/api/feed/popular?days=91", a).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이번 주의 플리: 리뷰 수가 기준 이상인 플리만, 평균 총점 순, 순위 1부터")
    void weeklyPlaylists() throws Exception {
        String[] users = new String[4];
        for (int i = 0; i < users.length; i++) {
            users[i] = signupAndGetToken("평가자" + i);
        }
        // great: 리뷰 3개 평균 5.0 / good: 리뷰 4개 평균 4.0 / small: 리뷰 1개 평균 5.0 (기준 미달) / 곡은 무관
        for (int i = 0; i < 3; i++) {
            postJson("/api/reviews", users[i], review("PLAYLIST", "great", "최고 플리", 5.0)).andExpect(status().isCreated());
        }
        for (int i = 0; i < 4; i++) {
            postJson("/api/reviews", users[i], review("PLAYLIST", "good", "좋은 플리", 4.0)).andExpect(status().isCreated());
        }
        postJson("/api/reviews", users[0], review("PLAYLIST", "small", "작은 플리", 5.0)).andExpect(status().isCreated());
        for (int i = 0; i < 4; i++) {
            postJson("/api/reviews", users[i], review("TRACK", "song", "곡", 5.0)).andExpect(status().isCreated());
        }

        getJson("/api/feed/weekly-playlists?minReviews=3", users[0])
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].spotifyId").value("great"))
                .andExpect(jsonPath("$[0].averageRating").value(5.0))
                .andExpect(jsonPath("$[0].item.name").value("최고 플리"))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].spotifyId").value("good"))
                .andExpect(jsonPath("$[1].reviewCount").value(4));

        // 기본값은 리뷰 50개 이상 → 지금은 없음
        getJson("/api/feed/weekly-playlists", users[0]).andExpect(jsonPath("$", hasSize(0)));
        getJson("/api/feed/weekly-playlists?minReviews=3&days=7", users[0]).andExpect(jsonPath("$", hasSize(2)));
        getJson("/api/feed/weekly-playlists?days=0", users[0]).andExpect(status().isBadRequest());
    }
}
