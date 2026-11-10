package com.musicreview.server.review;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewApiTest extends ApiTestSupport {

    static final String TRACK_REVIEW = """
            {
              "type": "TRACK",
              "spotifyId": "4uLU6hMCjMI75M1A2tKUQC",
              "item": { "name": "Never Gonna Give You Up", "subtitle": "Rick Astley", "imageUrl": "https://i.scdn.co/image/abc" },
              "rating": 4.5,
              "comment": "몇 번을 들어도 좋다",
              "tags": ["신나는", " 80년대 ", "신나는"]
            }
            """;

    static final String PLAYLIST_REVIEW = """
            {
              "type": "PLAYLIST",
              "spotifyId": "3cEYpjA9oz9GiPac4AsH4n",
              "item": { "name": "새벽 드라이브", "subtitle": "smedjan" },
              "rating": 4,
              "conceptScore": 5,
              "flowScore": 4.5,
              "discoveryScore": 3.5,
              "snapshotId": "SNAP1",
              "ownerSpotifyId": "smedjan"
            }
            """;

    String token;

    @BeforeEach
    void setUp() throws Exception {
        token = signupAndGetToken("리뷰어");
    }

    @Test
    @DisplayName("곡 리뷰 작성: 201, Location, 태그 공백 제거·중복 제거·정렬, 플리 점수는 null")
    void createTrackReview() throws Exception {
        postJson("/api/reviews", token, TRACK_REVIEW)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/reviews/")))
                .andExpect(jsonPath("$.type").value("TRACK"))
                .andExpect(jsonPath("$.rating").value(4.5))
                .andExpect(jsonPath("$.item.subtitle").value("Rick Astley"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.tags[0]").value("80년대"))
                .andExpect(jsonPath("$.tags[1]").value("신나는"))
                .andExpect(jsonPath("$.playlistScores").doesNotExist())
                .andExpect(jsonPath("$.isMine").value(true))
                .andExpect(jsonPath("$.author.nickname").value("리뷰어"))
                .andExpect(jsonPath("$.author.email").doesNotExist())
                .andExpect(jsonPath("$.createdAt", containsString("+09:00")));
    }

    @Test
    @DisplayName("플리 리뷰 작성: 점수는 소수 첫째 자리로 통일")
    void createPlaylistReview() throws Exception {
        postJson("/api/reviews", token, PLAYLIST_REVIEW)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4.0))
                .andExpect(jsonPath("$.playlistScores.concept").value(5.0))
                .andExpect(jsonPath("$.playlistScores.snapshotId").value("SNAP1"))
                .andExpect(jsonPath("$.playlistScores.ownerSpotifyId").value("smedjan"));
    }

    @Test
    @DisplayName("같은 항목에 두 번 쓰면 409 REVIEW_ALREADY_EXISTS")
    void duplicate() throws Exception {
        postJson("/api/reviews", token, TRACK_REVIEW).andExpect(status().isCreated());
        postJson("/api/reviews", token, TRACK_REVIEW)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("다른 사람은 같은 항목에 리뷰 가능")
    void otherUserSameItem() throws Exception {
        postJson("/api/reviews", token, TRACK_REVIEW).andExpect(status().isCreated());
        postJson("/api/reviews", signupAndGetToken("다른사람"), TRACK_REVIEW).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("곡 리뷰에 플리 전용 필드를 보내면 400 INVALID_REVIEW_FIELDS")
    void trackWithPlaylistFields() throws Exception {
        postJson("/api/reviews", token, """
                {"type":"TRACK","spotifyId":"abc123","item":{"name":"곡"},"rating":4.0,"flowScore":3.0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_FIELDS"));
    }

    @Test
    @DisplayName("플리 리뷰에 항목 점수가 빠지면 400 INVALID_REVIEW_FIELDS, 빠진 필드 이름 안내")
    void playlistMissingScores() throws Exception {
        postJson("/api/reviews", token, """
                {"type":"PLAYLIST","spotifyId":"pl1","item":{"name":"플리"},"rating":4.0,"conceptScore":4.0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_FIELDS"))
                .andExpect(jsonPath("$.message", containsString("flowScore")))
                .andExpect(jsonPath("$.message", containsString("ownerSpotifyId")));
    }

    @Test
    @DisplayName("별점이 0.5 단위가 아니거나 범위를 벗어나면 400")
    void invalidRating() throws Exception {
        for (String rating : new String[]{"4.3", "0", "5.5", "0.25"}) {
            postJson("/api/reviews", token, """
                    {"type":"ALBUM","spotifyId":"alb1","item":{"name":"앨범"},"rating":%s}
                    """.formatted(rating))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.errors[0].field").value("rating"));
        }
    }

    @Test
    @DisplayName("태그 6개, 잘못된 spotifyId, 없는 type은 400")
    void otherValidation() throws Exception {
        postJson("/api/reviews", token, """
                {"type":"TRACK","spotifyId":"t1","item":{"name":"곡"},"rating":3.0,"tags":["a","b","c","d","e","f"]}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("tags"));
        postJson("/api/reviews", token, """
                {"type":"TRACK","spotifyId":"../../etc","item":{"name":"곡"},"rating":3.0}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("spotifyId"));
        postJson("/api/reviews", token, """
                {"type":"ARTIST","spotifyId":"a1","item":{"name":"가수"},"rating":3.0}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("조회 → 수정(보낸 필드만) → 삭제 → 다시 조회하면 404")
    void readUpdateDelete() throws Exception {
        String url = "/api/reviews/" + createAndGetId(TRACK_REVIEW);

        getJson(url, token).andExpect(status().isOk()).andExpect(jsonPath("$.comment").value("몇 번을 들어도 좋다"));

        patchJson(url, token, """
                {"rating":5,"tags":["명곡"]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5.0))
                .andExpect(jsonPath("$.tags", hasSize(1)))
                .andExpect(jsonPath("$.comment").value("몇 번을 들어도 좋다"));

        patchJson(url, token, """
                {"comment":"","tags":[]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").doesNotExist())
                .andExpect(jsonPath("$.tags", hasSize(0)));

        deleteJson(url, token, null).andExpect(status().isNoContent());
        getJson(url, token).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
    }

    @Test
    @DisplayName("남의 리뷰를 수정·삭제하면 403, 조회는 가능하고 isMine=false")
    void othersReview() throws Exception {
        String url = "/api/reviews/" + createAndGetId(TRACK_REVIEW);
        String other = signupAndGetToken("남이");

        getJson(url, other).andExpect(status().isOk()).andExpect(jsonPath("$.isMine").value(false));
        patchJson(url, other, "{\"rating\":1.0}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REVIEW_FORBIDDEN"));
        deleteJson(url, other, null).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("곡 리뷰 수정에 플리 필드를 보내면 400, 플리 리뷰는 항목 점수·snapshotId 수정 가능")
    void updatePlaylistFields() throws Exception {
        String trackUrl = "/api/reviews/" + createAndGetId(TRACK_REVIEW);
        patchJson(trackUrl, token, "{\"snapshotId\":\"X\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_FIELDS"));

        String playlistUrl = "/api/reviews/" + createAndGetId(PLAYLIST_REVIEW);
        patchJson(playlistUrl, token, "{\"flowScore\":2.5,\"snapshotId\":\"SNAP2\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playlistScores.flow").value(2.5))
                .andExpect(jsonPath("$.playlistScores.concept").value(5.0))
                .andExpect(jsonPath("$.playlistScores.snapshotId").value("SNAP2"));
    }

    @Test
    @DisplayName("내 리뷰 일괄 조회: 내가 쓴 것만, type 대소문자 무관")
    void mine() throws Exception {
        createAndGetId(TRACK_REVIEW);
        postJson("/api/reviews", signupAndGetToken("남이"), TRACK_REVIEW.replace("4uLU6hMCjMI75M1A2tKUQC", "otherTrack"));

        getJson("/api/reviews/mine?type=track&spotifyIds=4uLU6hMCjMI75M1A2tKUQC,otherTrack,none", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].spotifyId").value("4uLU6hMCjMI75M1A2tKUQC"));

        getJson("/api/reviews/mine?type=song&spotifyIds=a", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ITEM_TYPE"));
    }

    @Test
    @DisplayName("없는 리뷰 id는 404, 숫자가 아니면 400")
    void notFound() throws Exception {
        getJson("/api/reviews/999999", token).andExpect(status().isNotFound());
        getJson("/api/reviews/abc", token).andExpect(status().isBadRequest());
    }

    private long createAndGetId(String json) throws Exception {
        Number id = read(postJson("/api/reviews", token, json).andExpect(status().isCreated()), "$.id");
        return id.longValue();
    }
}
