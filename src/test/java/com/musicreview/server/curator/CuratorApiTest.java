package com.musicreview.server.curator;

import com.musicreview.server.support.ApiTestSupport;
import com.musicreview.server.user.domain.User;
import com.musicreview.server.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 베스트 큐레이터 = Spotify를 연동한 사용자. 연동은 API 테스트(SpotifyApiTest)에서 따로 검증하고 여기선 DB로 바로 연결 */
class CuratorApiTest extends ApiTestSupport {

    @Autowired
    UserRepository userRepository;

    private String playlistReview(String playlistId, String owner, double rating) {
        return """
                {"type":"PLAYLIST","spotifyId":"%s","item":{"name":"플리 %s","subtitle":"주인"},"rating":%s,
                 "conceptScore":%s,"flowScore":4.0,"discoveryScore":3.0,"ownerSpotifyId":"%s"}
                """.formatted(playlistId, playlistId, rating, rating, owner);
    }

    /** 가입하고 Spotify 계정을 연결한 큐레이터의 user id */
    private long curator(String nickname, String spotifyUserId) throws Exception {
        String token = signupAndGetToken(nickname);
        long id = ((Number) read(getJson("/api/users/me", token), "$.id")).longValue();
        User user = userRepository.findById(id).orElseThrow();
        user.linkSpotify(spotifyUserId, nickname + " (Spotify)");
        userRepository.flush();
        return id;
    }

    @Test
    @DisplayName("랭킹: Spotify 연동한 사람만, 평균 총점 순, 같은 평균은 같은 순위(1,1,3)")
    void ranking() throws Exception {
        long alice = curator("앨리스", "alice");
        long bob = curator("밥이야", "bob");
        long carol = curator("캐럴", "carol");
        String a = signupAndGetToken("평가자a");
        String b = signupAndGetToken("평가자b");
        String c = signupAndGetToken("평가자c");

        // alice: 플리 2개, 리뷰 3개, 평균 5.0
        postJson("/api/reviews", a, playlistReview("p1", "alice", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, playlistReview("p1", "alice", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", c, playlistReview("p2", "alice", 5.0)).andExpect(status().isCreated());
        // bob: 리뷰 2개, 평균 5.0 → alice와 같은 1위
        postJson("/api/reviews", a, playlistReview("p3", "bob", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, playlistReview("p3", "bob", 5.0)).andExpect(status().isCreated());
        // carol: 평균 3.5 → 3위
        postJson("/api/reviews", a, playlistReview("p4", "carol", 3.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, playlistReview("p4", "carol", 4.0)).andExpect(status().isCreated());
        // 연동 안 한 주인(stranger)은 평균이 높아도 제외
        postJson("/api/reviews", a, playlistReview("p5", "stranger", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, playlistReview("p5", "stranger", 5.0)).andExpect(status().isCreated());

        getJson("/api/curators/ranking?minReviews=2", a)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].userId").value(alice))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].nickname").value("앨리스"))
                .andExpect(jsonPath("$[0].spotifyDisplayName").value("앨리스 (Spotify)"))
                .andExpect(jsonPath("$[0].playlistCount").value(2))
                .andExpect(jsonPath("$[0].reviewCount").value(3))
                .andExpect(jsonPath("$[1].userId").value(bob))
                .andExpect(jsonPath("$[1].rank").value(1))
                .andExpect(jsonPath("$[2].userId").value(carol))
                .andExpect(jsonPath("$[2].rank").value(3))
                .andExpect(jsonPath("$[2].averageRating").value(3.5));
    }

    @Test
    @DisplayName("큐레이터 프로필과 플리 목록(인기순: 리뷰 수 → 평균)")
    void profileAndPlaylists() throws Exception {
        long alice = curator("앨리스", "alice");
        String a = signupAndGetToken("평가자a");
        String b = signupAndGetToken("평가자b");
        postJson("/api/reviews", a, playlistReview("low", "alice", 2.0)).andExpect(status().isCreated());
        postJson("/api/reviews", a, playlistReview("hot", "alice", 4.0)).andExpect(status().isCreated());
        postJson("/api/reviews", b, playlistReview("hot", "alice", 5.0)).andExpect(status().isCreated());
        postJson("/api/reviews", a, playlistReview("top", "alice", 5.0)).andExpect(status().isCreated());

        getJson("/api/curators/" + alice, a)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("앨리스"))
                .andExpect(jsonPath("$.spotifyUserId").value("alice"))
                .andExpect(jsonPath("$.playlistCount").value(3))
                .andExpect(jsonPath("$.reviewCount").value(4))
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.email").doesNotExist());

        getJson("/api/curators/" + alice + "/playlists", a)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].spotifyId").value("hot"))
                .andExpect(jsonPath("$.content[0].reviewCount").value(2))
                .andExpect(jsonPath("$.content[1].spotifyId").value("top"))
                .andExpect(jsonPath("$.content[2].spotifyId").value("low"))
                .andExpect(jsonPath("$.content[0].item.name").value("플리 hot"));
    }

    @Test
    @DisplayName("Spotify 연동 안 한 사용자의 큐레이터 프로필은 404, 리뷰가 없어도 프로필은 0으로")
    void notCurator() throws Exception {
        String plain = signupAndGetToken("일반인");
        long plainId = ((Number) read(getJson("/api/users/me", plain), "$.id")).longValue();
        getJson("/api/curators/" + plainId, plain)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CURATOR_NOT_FOUND"));
        getJson("/api/curators/" + plainId + "/playlists", plain).andExpect(status().isNotFound());

        long fresh = curator("새큐레이터", "fresh");
        getJson("/api/curators/" + fresh, plain)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.averageRating").doesNotExist());
    }

    @Test
    @DisplayName("기본값 minReviews=3, 범위를 벗어난 값은 400")
    void defaultsAndValidation() throws Exception {
        String a = signupAndGetToken("기본값");
        getJson("/api/curators/ranking", a).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        getJson("/api/curators/ranking?minReviews=0", a).andExpect(status().isBadRequest());
        getJson("/api/curators/ranking?limit=101", a).andExpect(status().isBadRequest());
    }
}
