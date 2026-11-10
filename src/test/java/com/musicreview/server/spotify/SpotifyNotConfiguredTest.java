package com.musicreview.server.spotify;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** SPOTIFY_CLIENT_ID/SECRET 없이 실행한 서버 (기본 설정) */
class SpotifyNotConfiguredTest extends ApiTestSupport {

    @Test
    @DisplayName("Spotify 설정이 없으면 503 SPOTIFY_NOT_CONFIGURED, 다른 API는 정상")
    void notConfigured() throws Exception {
        String token = signupAndGetToken("설정없음");
        getJson("/api/spotify/tracks/abc", token)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SPOTIFY_NOT_CONFIGURED"));
        getJson("/api/users/me", token).andExpect(status().isOk());
    }
}
