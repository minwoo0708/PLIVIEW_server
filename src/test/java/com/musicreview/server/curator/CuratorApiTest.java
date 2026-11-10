package com.musicreview.server.curator;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CuratorApiTest extends ApiTestSupport {

    private String playlistReview(String playlistId, String owner, String ownerName, double rating) {
        return """
                {"type":"PLAYLIST","spotifyId":"%s","item":{"name":"플리 %s","subtitle":"%s"},"rating":%s,
                 "conceptScore":%s,"flowScore":4.0,"discoveryScore":3.0,"ownerSpotifyId":"%s"}
                """.formatted(playlistId, playlistId, ownerName, rating, rating, owner);
    }

    @Test
    @DisplayName("랭킹: 평균 총점 순, 반올림 평균이 같으면 같은 순위(1,1,3), 최소 리뷰 수 필터")
    void ranking() throws Exception {
        String a = signupAndGetToken("에이");
        String b = signupAndGetToken("비이");
        String c = signupAndGetToken("씨이");

        // alice: 플리 2개, 리뷰 3개, 평균 5.0
        postJson("/api/reviews", a, playlistReview("p1", "alice", "앨리스", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, playlistReview("p1", "alice", "앨리스", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", c, playlistReview("p2", "alice", "앨리스 새이름", 5.0)).andExpect(status().isCreated());
        // bob: 리뷰 2개, 평균 5.0 → alice와 같은 1위
        postJson("/api/reviews", a, playlistReview("p3", "bob", "밥", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, playlistReview("p3", "bob", "밥", 5.0)).andExpect(status().isCreated());
        // carol: 리뷰 2개, 평균 3.5 → 3위
        postJson("/api/reviews", a, playlistReview("p4", "carol", "캐럴", 3.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, playlistReview("p4", "carol", "캐럴", 4.0)).andExpect(status().isCreated());
        // dave: 리뷰 1개 → minReviews=2면 제외
        postJson("/api/reviews", a, playlistReview("p5", "dave", "데이브", 5.0)).andExpect(status().isCreated());
        // 곡 리뷰는 랭킹과 무관
        postJson("/api/reviews", a, """
                {"type":"TRACK","spotifyId":"t1","item":{"name":"곡"},"rating":1.0}
                """).andExpect(status().isCreated());

        getJson("/api/curators/ranking?minReviews=2", a)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].ownerSpotifyId").value("alice"))   // 같은 평균이면 리뷰 많은 쪽 먼저
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].ownerName").value("앨리스 새이름"))
                .andExpect(jsonPath("$[0].playlistCount").value(2))
                .andExpect(jsonPath("$[0].reviewCount").value(3))
                .andExpect(jsonPath("$[0].averageRating").value(5.0))
                .andExpect(jsonPath("$[0].averageScores.flow").value(4.0))
                .andExpect(jsonPath("$[1].ownerSpotifyId").value("bob"))
                .andExpect(jsonPath("$[1].rank").value(1))
                .andExpect(jsonPath("$[2].ownerSpotifyId").value("carol"))
                .andExpect(jsonPath("$[2].rank").value(3))
                .andExpect(jsonPath("$[2].averageRating").value(3.5));

        getJson("/api/curators/ranking?minReviews=1&limit=10", a)
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    @DisplayName("기본값 minReviews=3, 범위를 벗어난 값은 400")
    void defaultsAndValidation() throws Exception {
        String a = signupAndGetToken("기본");
        getJson("/api/curators/ranking", a).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getJson("/api/curators/ranking?minReviews=0", a).andExpect(status().isBadRequest());
        getJson("/api/curators/ranking?limit=101", a).andExpect(status().isBadRequest());
    }
}
