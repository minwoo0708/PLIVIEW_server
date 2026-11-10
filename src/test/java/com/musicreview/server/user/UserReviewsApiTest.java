package com.musicreview.server.user;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserReviewsApiTest extends ApiTestSupport {

    private String trackReview(String spotifyId, double rating) {
        return """
                {"type":"TRACK","spotifyId":"%s","item":{"name":"곡 %s"},"rating":%s}
                """.formatted(spotifyId, spotifyId, rating);
    }

    @Test
    @DisplayName("유저 프로필: 공개 정보(이메일 없음), 전체 통계, 필터된 리뷰 목록")
    void userReviews() throws Exception {
        String me = signupAndGetToken("프로필");
        long myId = ((Number) read(getJson("/api/users/me", me), "$.id")).longValue();
        postJson("/api/reviews", me, trackReview("t1", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", me, trackReview("t2", 4.5)).andExpect(status().isCreated());
        postJson("/api/reviews", me, trackReview("t3", 3.0)).andExpect(status().isCreated());
        postJson("/api/reviews", me, """
                {"type":"ALBUM","spotifyId":"a1","item":{"name":"앨범"},"rating":5.0}
                """).andExpect(status().isCreated());

        String viewer = signupAndGetToken("구경꾼");
        getJson("/api/users/" + myId + "/reviews", viewer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.nickname").value("프로필"))
                .andExpect(jsonPath("$.user.email").doesNotExist())
                .andExpect(jsonPath("$.stats.reviewCount").value(4))
                .andExpect(jsonPath("$.stats.averageRating").value(4.4))     // 17.5 / 4 = 4.375
                .andExpect(jsonPath("$.stats.ratingDistribution['5.0']").value(2))
                .andExpect(jsonPath("$.stats.ratingDistribution['4.5']").value(1))
                .andExpect(jsonPath("$.stats.countByType.TRACK").value(3))
                .andExpect(jsonPath("$.stats.countByType.ALBUM").value(1))
                .andExpect(jsonPath("$.stats.countByType.PLAYLIST").value(0))
                .andExpect(jsonPath("$.reviews.content", hasSize(4)))
                .andExpect(jsonPath("$.reviews.content[0].isMine").value(false));

        // 리뷰 기반 플리 생성용: 4.5점 이상 곡만. 통계는 그대로 전체 기준
        getJson("/api/users/" + myId + "/reviews?type=TRACK&minRating=4.5", me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews.content", hasSize(2)))
                .andExpect(jsonPath("$.reviews.totalElements").value(2))
                .andExpect(jsonPath("$.stats.reviewCount").value(4));
    }

    @Test
    @DisplayName("리뷰가 없는 유저: 0개, 평균 null, 분포·타입별 모두 0")
    void emptyUser() throws Exception {
        String me = signupAndGetToken("새내기");
        long myId = ((Number) read(getJson("/api/users/me", me), "$.id")).longValue();
        getJson("/api/users/" + myId + "/reviews", me)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.reviewCount").value(0))
                .andExpect(jsonPath("$.stats.averageRating").doesNotExist())
                .andExpect(jsonPath("$.stats.ratingDistribution['3.0']").value(0))
                .andExpect(jsonPath("$.stats.countByType.TRACK").value(0))
                .andExpect(jsonPath("$.reviews.content", hasSize(0)));
    }

    @Test
    @DisplayName("없는 유저 404, 잘못된 minRating·type 400")
    void errors() throws Exception {
        String me = signupAndGetToken("오류");
        getJson("/api/users/999999/reviews", me)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        getJson("/api/users/1/reviews?minRating=4.3", me)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("minRating"));
        long myId = ((Number) read(getJson("/api/users/me", me), "$.id")).longValue();
        getJson("/api/users/" + myId + "/reviews?type=song", me)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ITEM_TYPE"));
    }
}
