package com.musicreview.server.item;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemApiTest extends ApiTestSupport {

    private static final String PLAYLIST = "pl123";

    private String playlistReview(double rating, double concept, double flow, double discovery) {
        return """
                {"type":"PLAYLIST","spotifyId":"%s","item":{"name":"플리"},"rating":%s,
                 "conceptScore":%s,"flowScore":%s,"discoveryScore":%s,"ownerSpotifyId":"owner1"}
                """.formatted(PLAYLIST, rating, concept, flow, discovery);
    }

    @Test
    @DisplayName("리뷰가 없으면 요약은 0개, 평균 null, 분포 10칸 모두 0, 목록은 빈 페이지")
    void emptyItem() throws Exception {
        String token = signupAndGetToken("빈칸");
        getJson("/api/items/track/nothing/summary", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRACK"))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.averageRating").doesNotExist())
                .andExpect(jsonPath("$.ratingDistribution['0.5']").value(0))
                .andExpect(jsonPath("$.ratingDistribution['5.0']").value(0))
                .andExpect(jsonPath("$.playlistAverages").doesNotExist())
                .andExpect(jsonPath("$.myReview").doesNotExist());

        getJson("/api/items/track/nothing/reviews", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("플리 요약: 평균(반올림), 분포, 항목별 평균, 내 리뷰")
    void playlistSummary() throws Exception {
        String me = signupAndGetToken("나야");
        postJson("/api/reviews", me, playlistReview(5.0, 5.0, 4.0, 3.0)).andExpect(status().isCreated());
        postJson("/api/reviews", signupAndGetToken("둘째"), playlistReview(4.0, 4.0, 4.0, 4.0)).andExpect(status().isCreated());
        postJson("/api/reviews", signupAndGetToken("셋째"), playlistReview(4.0, 3.0, 3.5, 4.5)).andExpect(status().isCreated());

        getJson("/api/items/PLAYLIST/" + PLAYLIST + "/summary", me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(3))
                .andExpect(jsonPath("$.averageRating").value(4.3))       // 13 / 3 = 4.333
                .andExpect(jsonPath("$.ratingDistribution['4.0']").value(2))
                .andExpect(jsonPath("$.ratingDistribution['5.0']").value(1))
                .andExpect(jsonPath("$.ratingDistribution['4.5']").value(0))
                .andExpect(jsonPath("$.playlistAverages.concept").value(4.0))
                .andExpect(jsonPath("$.playlistAverages.flow").value(3.8))  // 11.5 / 3 = 3.833
                .andExpect(jsonPath("$.playlistAverages.discovery").value(3.8))
                .andExpect(jsonPath("$.myReview.rating").value(5.0))
                .andExpect(jsonPath("$.myReview.isMine").value(true));
    }

    @Test
    @DisplayName("리뷰 목록: 정렬(rating_desc), 페이지 크기, hasNext")
    void listSortAndPage() throws Exception {
        String token = signupAndGetToken("목록");
        postJson("/api/reviews", token, playlistReview(3.0, 3.0, 3.0, 3.0));
        postJson("/api/reviews", signupAndGetToken("비비"), playlistReview(5.0, 3.0, 3.0, 3.0));
        postJson("/api/reviews", signupAndGetToken("씨씨"), playlistReview(1.0, 3.0, 3.0, 3.0));

        getJson("/api/items/playlist/" + PLAYLIST + "/reviews?sort=rating_desc&size=2", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].rating").value(5.0))
                .andExpect(jsonPath("$.content[1].rating").value(3.0))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        getJson("/api/items/playlist/" + PLAYLIST + "/reviews?sort=rating_asc", token)
                .andExpect(jsonPath("$.content[0].rating").value(1.0));
    }

    @Test
    @DisplayName("잘못된 type, sort, size, spotifyId는 400")
    void invalidParams() throws Exception {
        String token = signupAndGetToken("검증");
        getJson("/api/items/artist/abc/summary", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ITEM_TYPE"));
        getJson("/api/items/track/abc/reviews?sort=popular", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        getJson("/api/items/track/abc/reviews?size=51", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
        getJson("/api/items/track/a-b/summary", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("spotifyId"));
    }
}
