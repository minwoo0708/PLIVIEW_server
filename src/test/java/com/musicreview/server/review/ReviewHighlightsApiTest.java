package com.musicreview.server.review;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 앨범·플리 리뷰의 최고의 곡 / 워스트 곡 / 과소평가된 곡 / 최고의 순간 / 전체 평가 */
class ReviewHighlightsApiTest extends ApiTestSupport {

    String token;

    @BeforeEach
    void setUp() throws Exception {
        token = signupAndGetToken("하이라이트");
    }

    @Test
    @DisplayName("앨범 리뷰: 하이라이트 5가지 저장, 여러 줄 전체 평가 유지")
    void albumHighlights() throws Exception {
        postJson("/api/reviews", token, """
                {"type":"ALBUM","spotifyId":"alb1","item":{"name":"앨범"},"rating":4.5,
                 "bestTrack":{"spotifyId":"t1","name":"타이틀곡"},
                 "worstTrack":{"spotifyId":"t2","name":"아쉬운 곡"},
                 "underratedTrack":{"spotifyId":"t3","name":"숨은 명곡"},
                 "bestMoment":"2:31 브릿지",
                 "body":"첫 줄\\n둘째 줄"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.highlights.bestTrack.spotifyId").value("t1"))
                .andExpect(jsonPath("$.highlights.bestTrack.name").value("타이틀곡"))
                .andExpect(jsonPath("$.highlights.worstTrack.name").value("아쉬운 곡"))
                .andExpect(jsonPath("$.highlights.underratedTrack.name").value("숨은 명곡"))
                .andExpect(jsonPath("$.highlights.bestMoment").value("2:31 브릿지"))
                .andExpect(jsonPath("$.highlights.body").value("첫 줄\n둘째 줄"));
    }

    @Test
    @DisplayName("하이라이트는 모두 생략 가능, 앨범이면 highlights 객체는 있고 값은 null")
    void allOptional() throws Exception {
        postJson("/api/reviews", token, """
                {"type":"ALBUM","spotifyId":"alb2","item":{"name":"앨범"},"rating":3.0}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.highlights").exists())
                .andExpect(jsonPath("$.highlights.bestTrack").doesNotExist())
                .andExpect(jsonPath("$.highlights.body").doesNotExist());
    }

    @Test
    @DisplayName("곡 리뷰에는 하이라이트 금지, 응답 highlights는 null")
    void trackHasNoHighlights() throws Exception {
        postJson("/api/reviews", token, """
                {"type":"TRACK","spotifyId":"t9","item":{"name":"곡"},"rating":4.0,"bestMoment":"후렴"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_FIELDS"));
        postJson("/api/reviews", token, """
                {"type":"TRACK","spotifyId":"t9","item":{"name":"곡"},"rating":4.0,"comment":"한 줄 평"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.highlights").doesNotExist());
    }

    @Test
    @DisplayName("과소평가된 곡은 앨범만: 플리에 보내면 400, 최고·워스트 곡은 플리도 가능")
    void underratedOnlyForAlbum() throws Exception {
        String playlist = """
                {"type":"PLAYLIST","spotifyId":"pl1","item":{"name":"플리"},"rating":4.0,
                 "conceptScore":4,"flowScore":4,"discoveryScore":4,"ownerSpotifyId":"owner",
                 %s}
                """;
        postJson("/api/reviews", token, playlist.formatted("\"underratedTrack\":{\"spotifyId\":\"t1\",\"name\":\"곡\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REVIEW_FIELDS"));
        postJson("/api/reviews", token, playlist.formatted(
                "\"bestTrack\":{\"spotifyId\":\"t1\",\"name\":\"최고\"},\"worstTrack\":{\"spotifyId\":\"t2\",\"name\":\"워스트\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.highlights.bestTrack.name").value("최고"))
                .andExpect(jsonPath("$.highlights.underratedTrack").doesNotExist());
    }

    @Test
    @DisplayName("검증: 곡 id 형식, 최고의 순간 100자, 전체 평가 2000자")
    void validation() throws Exception {
        postJson("/api/reviews", token, """
                {"type":"ALBUM","spotifyId":"alb3","item":{"name":"앨범"},"rating":3.0,
                 "bestTrack":{"spotifyId":"a-b","name":"곡"}}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bestTrack.spotifyId"));
        postJson("/api/reviews", token, """
                {"type":"ALBUM","spotifyId":"alb3","item":{"name":"앨범"},"rating":3.0,"bestMoment":"%s"}
                """.formatted("가".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bestMoment"));
        postJson("/api/reviews", token, """
                {"type":"ALBUM","spotifyId":"alb3","item":{"name":"앨범"},"rating":3.0,"body":"%s"}
                """.formatted("a".repeat(2001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("body"));
    }

    @Test
    @DisplayName("clear: 곡 하이라이트도 지울 수 있고, 값과 같이 보내거나 모르는 이름이면 400")
    void clearFields() throws Exception {
        Number id = read(postJson("/api/reviews", token, """
                {"type":"ALBUM","spotifyId":"alb5","item":{"name":"앨범"},"rating":4.0,"comment":"한 줄",
                 "bestTrack":{"spotifyId":"t1","name":"최고"},"worstTrack":{"spotifyId":"t2","name":"워스트"},
                 "underratedTrack":{"spotifyId":"t3","name":"과소"},"bestMoment":"인트로","body":"본문"}
                """).andExpect(status().isCreated()), "$.id");

        patchJson("/api/reviews/" + id, token, """
                {"clear":["bestTrack","underratedTrack","comment","body"]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.highlights.bestTrack").doesNotExist())
                .andExpect(jsonPath("$.highlights.underratedTrack").doesNotExist())
                .andExpect(jsonPath("$.highlights.worstTrack.name").value("워스트"))
                .andExpect(jsonPath("$.highlights.bestMoment").value("인트로"))
                .andExpect(jsonPath("$.highlights.body").doesNotExist())
                .andExpect(jsonPath("$.comment").doesNotExist());

        patchJson("/api/reviews/" + id, token, """
                {"bestMoment":"새 순간","clear":["bestMoment"]}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        patchJson("/api/reviews/" + id, token, """
                {"clear":["rating"]}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("곡 리뷰에서 하이라이트를 clear하면 400, comment clear는 가능")
    void clearOnTrack() throws Exception {
        Number id = read(postJson("/api/reviews", token, """
                {"type":"TRACK","spotifyId":"t7","item":{"name":"곡"},"rating":3.0,"comment":"좋다"}
                """).andExpect(status().isCreated()), "$.id");
        patchJson("/api/reviews/" + id, token, "{\"clear\":[\"bestTrack\"]}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REVIEW_FIELDS"));
        patchJson("/api/reviews/" + id, token, "{\"clear\":[\"comment\"]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.comment").doesNotExist());
    }

    @Test
    @DisplayName("수정: 보낸 하이라이트만 변경, 빈 문자열이면 최고의 순간·전체 평가 삭제")
    void update() throws Exception {
        Number id = read(postJson("/api/reviews", token, """
                {"type":"ALBUM","spotifyId":"alb4","item":{"name":"앨범"},"rating":4.0,
                 "bestTrack":{"spotifyId":"t1","name":"원래 최고"},"bestMoment":"인트로","body":"좋다"}
                """).andExpect(status().isCreated()), "$.id");

        patchJson("/api/reviews/" + id, token, """
                {"worstTrack":{"spotifyId":"t5","name":"새 워스트"},"bestMoment":""}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.highlights.bestTrack.name").value("원래 최고"))
                .andExpect(jsonPath("$.highlights.worstTrack.name").value("새 워스트"))
                .andExpect(jsonPath("$.highlights.bestMoment").doesNotExist())
                .andExpect(jsonPath("$.highlights.body").value("좋다"));
    }
}
