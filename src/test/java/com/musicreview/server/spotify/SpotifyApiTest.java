package com.musicreview.server.spotify;

import com.musicreview.server.support.ApiTestSupport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 Spotify 대신 로컬 가짜 서버(JDK HttpServer)를 띄워서
 * 토큰 발급·캐시, 요청 전달, 오류 변환을 검증합니다.
 */
class SpotifyApiTest extends ApiTestSupport {

    private static final HttpServer FAKE_SPOTIFY;
    private static final AtomicInteger TOKEN_CALLS = new AtomicInteger();
    private static final List<String> API_REQUESTS = new CopyOnWriteArrayList<>();

    static {
        try {
            FAKE_SPOTIFY = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        FAKE_SPOTIFY.createContext("/api/token", exchange -> {
            TOKEN_CALLS.incrementAndGet();
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (!"Basic dGVzdC1pZDp0ZXN0LXNlY3JldA==".equals(auth)) { // test-id:test-secret
                respond(exchange, 400, "{\"error\":\"invalid_client\"}");
                return;
            }
            respond(exchange, 200, "{\"access_token\":\"app-token\",\"token_type\":\"Bearer\",\"expires_in\":3600}");
        });
        FAKE_SPOTIFY.createContext("/v1", exchange -> {
            API_REQUESTS.add(exchange.getRequestURI().getRawPath() + "?" + exchange.getRequestURI().getRawQuery());
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if ("/v1/me".equals(exchange.getRequestURI().getPath())) {
                // 사용자 Spotify 토큰으로 계정 확인 (연동)
                switch (String.valueOf(auth)) {
                    case "Bearer user-token-alice" -> respond(exchange, 200, "{\"id\":\"alice_sp\",\"display_name\":\"Alice\"}");
                    case "Bearer user-token-noname" -> respond(exchange, 200, "{\"id\":\"noname_sp\",\"display_name\":null}");
                    case "Bearer not-allowlisted" -> respond(exchange, 403, "{}");
                    default -> respond(exchange, 401, "{}");
                }
                return;
            }
            if (!"Bearer app-token".equals(auth)) {
                respond(exchange, 401, "{}");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            switch (path) {
                case "/v1/search" -> respond(exchange, 200, "{\"tracks\":{\"items\":[{\"id\":\"t1\",\"name\":\"검색결과\"}]}}");
                case "/v1/tracks/missing" -> respond(exchange, 404, "{\"error\":{\"status\":404}}");
                case "/v1/tracks/limited" -> respond(exchange, 429, "{}");
                case "/v1/tracks/broken" -> respond(exchange, 500, "{}");
                default -> respond(exchange, 200, "{\"id\":\"" + path.substring(path.lastIndexOf('/') + 1) + "\"}");
            }
        });
        FAKE_SPOTIFY.start();
    }

    @DynamicPropertySource
    static void spotifyProperties(DynamicPropertyRegistry registry) {
        String base = "http://127.0.0.1:" + FAKE_SPOTIFY.getAddress().getPort();
        registry.add("app.spotify.client-id", () -> "test-id");
        registry.add("app.spotify.client-secret", () -> "test-secret");
        registry.add("app.spotify.api-base-url", () -> base + "/v1");
        registry.add("app.spotify.token-url", () -> base + "/api/token");
    }

    @AfterAll
    static void stopServer() {
        FAKE_SPOTIFY.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    String token;

    @BeforeEach
    void setUp() throws Exception {
        token = signupAndGetToken("검색러");
        API_REQUESTS.clear();
    }

    @Test
    @DisplayName("검색: Spotify 응답을 그대로 전달, 쿼리는 엄격하게 인코딩, market=KR")
    void search() throws Exception {
        search("C++ 아이유", "TRACK", "5")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks.items[0].name").value("검색결과"));

        String sent = API_REQUESTS.getFirst();
        assertThat(sent).startsWith("/v1/search?");
        assertThat(sent).contains("q=C%2B%2B%20").contains("type=track").contains("limit=5").contains("market=KR");
        assertThat(URLDecoder.decode(sent.substring(sent.indexOf("q=") + 2).split("&")[0], StandardCharsets.UTF_8))
                .isEqualTo("C++ 아이유");
    }

    @Test
    @DisplayName("앱 토큰은 캐시해서 여러 요청에 재사용")
    void tokenIsCached() throws Exception {
        int before = TOKEN_CALLS.get();
        getJson("/api/spotify/tracks/abc", token).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("abc"));
        getJson("/api/spotify/albums/def", token).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("def"));
        getJson("/api/spotify/playlists/ghi", token).andExpect(status().isOk());
        getJson("/api/spotify/artists/jkl/albums?limit=10", token).andExpect(status().isOk());
        assertThat(TOKEN_CALLS.get() - before).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Spotify 404 → 404 SPOTIFY_NOT_FOUND, 429 → 429, 500 → 502")
    void errorTranslation() throws Exception {
        getJson("/api/spotify/tracks/missing", token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SPOTIFY_NOT_FOUND"));
        getJson("/api/spotify/tracks/limited", token)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("SPOTIFY_RATE_LIMITED"));
        getJson("/api/spotify/tracks/broken", token)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SPOTIFY_UNAVAILABLE"));
    }

    @Test
    @DisplayName("잘못된 type, 빈 검색어, 이상한 id는 Spotify를 부르지 않고 400")
    void validation() throws Exception {
        search("abc", "show", "20").andExpect(status().isBadRequest());
        search("   ", "track", "20").andExpect(status().isBadRequest());
        search("abc", "track", "51").andExpect(status().isBadRequest());
        getJson("/api/spotify/tracks/a-b", token).andExpect(status().isBadRequest());
        assertThat(API_REQUESTS).isEmpty();
    }

    /** URL 문자열 대신 param()으로 넘겨야 MockMvc가 값을 다시 인코딩하지 않음 */
    private org.springframework.test.web.servlet.ResultActions search(String q, String type, String limit) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/spotify/search")
                .param("q", q).param("type", type).param("limit", limit)
                .header("Authorization", "Bearer " + token));
    }

    @Test
    @DisplayName("Spotify 연동: 계정 확인 후 저장, 프로필에도 표시, 해제하면 null")
    void linkAndUnlink() throws Exception {
        putJson("/api/users/me/spotify", token, "{\"spotifyAccessToken\":\"user-token-alice\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotify.userId").value("alice_sp"))
                .andExpect(jsonPath("$.spotify.displayName").value("Alice"));

        long myId = ((Number) read(getJson("/api/users/me", token)
                .andExpect(jsonPath("$.spotify.userId").value("alice_sp")), "$.id")).longValue();
        getJson("/api/users/" + myId + "/reviews", token)
                .andExpect(jsonPath("$.user.spotify.displayName").value("Alice"));

        // 같은 계정으로 다시 연동해도 그대로 성공
        putJson("/api/users/me/spotify", token, "{\"spotifyAccessToken\":\"user-token-alice\"}")
                .andExpect(status().isOk());

        deleteJson("/api/users/me/spotify", token, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotify").doesNotExist());
    }

    @Test
    @DisplayName("이미 다른 PLIVIEW 계정에 연동된 Spotify 계정이면 409")
    void alreadyLinked() throws Exception {
        putJson("/api/users/me/spotify", token, "{\"spotifyAccessToken\":\"user-token-alice\"}").andExpect(status().isOk());
        putJson("/api/users/me/spotify", signupAndGetToken("다른사람"), "{\"spotifyAccessToken\":\"user-token-alice\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SPOTIFY_ALREADY_LINKED"));
    }

    @Test
    @DisplayName("잘못된 토큰 401, 개발 모드 미등록 계정 401(안내 문구), display_name 없으면 id로 표시")
    void linkErrors() throws Exception {
        putJson("/api/users/me/spotify", token, "{\"spotifyAccessToken\":\"expired\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SPOTIFY_TOKEN_INVALID"));
        putJson("/api/users/me/spotify", token, "{\"spotifyAccessToken\":\"not-allowlisted\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("대시보드")));
        putJson("/api/users/me/spotify", token, "{\"spotifyAccessToken\":\" \"}")
                .andExpect(status().isBadRequest());
        putJson("/api/users/me/spotify", token, "{\"spotifyAccessToken\":\"user-token-noname\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotify.displayName").value("noname_sp"));
    }

    private org.springframework.test.web.servlet.ResultActions putJson(String url, String bearer, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(json)
                .header("Authorization", "Bearer " + bearer));
    }

    @Test
    @DisplayName("로그인하지 않으면 401")
    void requiresLogin() throws Exception {
        getJson("/api/spotify/search?q=abc&type=track", null).andExpect(status().isUnauthorized());
    }
}
