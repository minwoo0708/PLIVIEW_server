package com.musicreview.server.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H2(local 프로필) 위에서 실제 HTTP 요청처럼 API를 호출하는 통합 테스트 기반.
 * 각 테스트는 트랜잭션 안에서 실행되고 끝나면 롤백됩니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    private int userSeq = 0;

    /** 새 사용자를 가입시키고 access token을 돌려줍니다. */
    protected String signupAndGetToken(String nickname) throws Exception {
        String email = "user" + (++userSeq) + "_" + System.nanoTime() + "@example.com";
        return signupAndGetToken(email, "music1234", nickname);
    }

    protected String signupAndGetToken(String email, String password, String nickname) throws Exception {
        String body = """
                {"email":"%s","password":"%s","nickname":"%s"}
                """.formatted(email, password, nickname);
        String response = mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    protected ResultActions getJson(String url, String token) throws Exception {
        var request = get(url);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    protected ResultActions postJson(String url, String token, String json) throws Exception {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    protected ResultActions patchJson(String url, String token, String json) throws Exception {
        var request = patch(url).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    protected ResultActions deleteJson(String url, String token, String json) throws Exception {
        var request = delete(url);
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    protected static <T> T read(ResultActions result, String jsonPath) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), jsonPath);
    }
}
