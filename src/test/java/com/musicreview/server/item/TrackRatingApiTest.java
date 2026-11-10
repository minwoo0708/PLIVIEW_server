package com.musicreview.server.item;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrackRatingApiTest extends ApiTestSupport {

    private String trackReview(String id, double rating) {
        return """
                {"type":"TRACK","spotifyId":"%s","item":{"name":"곡"},"rating":%s}
                """.formatted(id, rating);
    }

    @Test
    @DisplayName("곡별 평점 일괄 조회: 요청 순서 유지, 리뷰 없는 곡은 0개·null, 내 별점 포함")
    void ratings() throws Exception {
        String me = signupAndGetToken("나에요");
        String other = signupAndGetToken("남이야");
        postJson("/api/reviews", me, trackReview("t1", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", other, trackReview("t1", 4.0)).andExpect(status().isCreated());
        postJson("/api/reviews", other, trackReview("t2", 3.5)).andExpect(status().isCreated());
        // 같은 id라도 앨범 리뷰는 섞이면 안 됨
        postJson("/api/reviews", other, """
                {"type":"ALBUM","spotifyId":"t3","item":{"name":"앨범"},"rating":1.0}
                """).andExpect(status().isCreated());

        getJson("/api/items/tracks/ratings?spotifyIds=t2,t1,t3,t2", me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].spotifyId").value("t2"))
                .andExpect(jsonPath("$[0].reviewCount").value(1))
                .andExpect(jsonPath("$[0].averageRating").value(3.5))
                .andExpect(jsonPath("$[0].myRating").doesNotExist())
                .andExpect(jsonPath("$[1].spotifyId").value("t1"))
                .andExpect(jsonPath("$[1].reviewCount").value(2))
                .andExpect(jsonPath("$[1].averageRating").value(4.5))
                .andExpect(jsonPath("$[1].myRating").value(5.0))
                .andExpect(jsonPath("$[2].spotifyId").value("t3"))
                .andExpect(jsonPath("$[2].reviewCount").value(0))
                .andExpect(jsonPath("$[2].averageRating").doesNotExist());
    }

    @Test
    @DisplayName("잘못된 id나 100개 초과는 400")
    void validation() throws Exception {
        String me = signupAndGetToken("검증용");
        getJson("/api/items/tracks/ratings?spotifyIds=ok1,a-b", me)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        String many = String.join(",", java.util.stream.IntStream.rangeClosed(1, 101).mapToObj(i -> "t" + i).toList());
        getJson("/api/items/tracks/ratings?spotifyIds=" + many, me)
                .andExpect(status().isBadRequest());
    }
}
