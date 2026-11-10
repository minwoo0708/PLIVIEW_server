package com.musicreview.server.auth;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    private static final String SIGNUP = "/api/auth/signup";
    private static final String LOGIN = "/api/auth/login";

    @Test
    @DisplayName("hello는 토큰 없이 200")
    void helloIsPublic() throws Exception {
        getJson("/api/hello", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("hello"));
    }

    @Test
    @DisplayName("회원가입하면 201과 토큰, 이메일은 소문자로 저장, 비밀번호는 응답에 없음")
    void signup() throws Exception {
        postJson(SIGNUP, null, """
                {"email":"  Me@Example.COM ","password":"music1234","nickname":"  음악덕후  "}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(604800))
                .andExpect(jsonPath("$.user.email").value("me@example.com"))
                .andExpect(jsonPath("$.user.nickname").value("음악덕후"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("같은 이메일(대소문자만 다름)로 다시 가입하면 409")
    void duplicateEmail() throws Exception {
        signupAndGetToken("dup@example.com", "music1234", "첫번째");
        postJson(SIGNUP, null, """
                {"email":"DUP@example.com","password":"music1234","nickname":"두번째"}
                """)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("가입 검증 실패는 400 INVALID_INPUT과 필드별 사유")
    void signupValidation() throws Exception {
        postJson(SIGNUP, null, """
                {"email":"not-an-email","password":"short","nickname":" a "}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("nickname")));
    }

    @Test
    @DisplayName("비밀번호에 숫자가 없으면 400")
    void passwordNeedsDigit() throws Exception {
        postJson(SIGNUP, null, """
                {"email":"a@example.com","password":"onlyletters","nickname":"닉네임"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    @DisplayName("로그인 성공하면 토큰으로 내 정보 조회 가능")
    void loginThenMe() throws Exception {
        signupAndGetToken("login@example.com", "music1234", "로그인");
        var result = postJson(LOGIN, null, """
                {"email":"LOGIN@example.com","password":"music1234"}
                """).andExpect(status().isOk());
        String token = read(result, "$.accessToken");

        getJson("/api/users/me", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    @DisplayName("비밀번호가 틀리거나 없는 이메일이면 똑같이 401 LOGIN_FAILED")
    void loginFailed() throws Exception {
        signupAndGetToken("wrong@example.com", "music1234", "틀림");
        postJson(LOGIN, null, """
                {"email":"wrong@example.com","password":"music9999"}
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        postJson(LOGIN, null, """
                {"email":"nobody@example.com","password":"music1234"}
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    @DisplayName("토큰 없이 보호된 API는 401 UNAUTHORIZED (JSON 형식)")
    void protectedWithoutToken() throws Exception {
        getJson("/api/users/me", null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("위조된 토큰은 401")
    void forgedToken() throws Exception {
        getJson("/api/users/me", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.forged")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("만료·위조 토큰을 붙인 채 로그인해도 로그인은 됨")
    void staleTokenIgnoredOnLogin() throws Exception {
        signupAndGetToken("stale@example.com", "music1234", "만료");
        postJson(LOGIN, "garbage.token.value", """
                {"email":"stale@example.com","password":"music1234"}
                """)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잘못된 JSON은 400 INVALID_INPUT")
    void malformedJson() throws Exception {
        postJson(LOGIN, null, "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("Flutter 웹(localhost 다른 포트)의 CORS preflight 허용")
    void corsPreflight() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .options("/api/auth/login")
                        .header("Origin", "http://localhost:53421")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:53421"));
    }
}
