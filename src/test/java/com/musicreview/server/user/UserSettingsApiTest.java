package com.musicreview.server.user;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserSettingsApiTest extends ApiTestSupport {

    private org.springframework.test.web.servlet.ResultActions putSettings(String token, String json) throws Exception {
        var request = put("/api/users/me/settings").contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    @Test
    @DisplayName("한 번도 저장하지 않았으면 기본값 SYSTEM, 1.0")
    void defaults() throws Exception {
        String token = signupAndGetToken("기본설정");
        getJson("/api/users/me/settings", token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("SYSTEM"))
                .andExpect(jsonPath("$.textScale").value(1.0));
    }

    @Test
    @DisplayName("PUT으로 저장하고 GET으로 다시 읽으면 같은 값")
    void saveAndRead() throws Exception {
        String token = signupAndGetToken("저장함");
        putSettings(token, "{\"themeMode\":\"DARK\",\"textScale\":1.15}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.textScale").value(1.15));
        getJson("/api/users/me/settings", token)
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.textScale").value(1.15));
    }

    @Test
    @DisplayName("보낸 항목만 바뀌고, 둘 다 안 보내도 현재 값 그대로 200")
    void partialUpdate() throws Exception {
        String token = signupAndGetToken("부분수정");
        putSettings(token, "{\"themeMode\":\"LIGHT\",\"textScale\":1.3}").andExpect(status().isOk());

        putSettings(token, "{\"themeMode\":\"DARK\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.textScale").value(1.3));

        putSettings(token, "{\"textScale\":0.9}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.textScale").value(0.9));

        putSettings(token, "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.textScale").value(0.9));
    }

    @Test
    @DisplayName("경계값 0.8과 1.5는 허용, 소수 셋째 자리는 둘째 자리로 반올림")
    void boundariesAndRounding() throws Exception {
        String token = signupAndGetToken("경계값");
        putSettings(token, "{\"textScale\":0.8}").andExpect(status().isOk()).andExpect(jsonPath("$.textScale").value(0.8));
        putSettings(token, "{\"textScale\":1.5}").andExpect(status().isOk()).andExpect(jsonPath("$.textScale").value(1.5));
        putSettings(token, "{\"textScale\":1.153}").andExpect(status().isOk()).andExpect(jsonPath("$.textScale").value(1.15));
    }

    @Test
    @DisplayName("범위 밖 배율은 400 INVALID_INPUT, 값은 바뀌지 않음")
    void outOfRange() throws Exception {
        String token = signupAndGetToken("범위밖");
        for (String bad : new String[]{"0.79", "1.51", "0", "-1", "3"}) {
            putSettings(token, "{\"textScale\":" + bad + "}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                    .andExpect(jsonPath("$.errors[0].field").value("textScale"));
        }
        getJson("/api/users/me/settings", token).andExpect(jsonPath("$.textScale").value(1.0));
    }

    @Test
    @DisplayName("themeMode에 허용되지 않는 값이나 문자열 배율은 400")
    void invalidValues() throws Exception {
        String token = signupAndGetToken("잘못된값");
        putSettings(token, "{\"themeMode\":\"PURPLE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        putSettings(token, "{\"themeMode\":\"dark\"}")
                .andExpect(status().isBadRequest());
        putSettings(token, "{\"textScale\":\"크게\"}")
                .andExpect(status().isBadRequest());
        getJson("/api/users/me/settings", token).andExpect(jsonPath("$.themeMode").value("SYSTEM"));
    }

    @Test
    @DisplayName("사용자마다 따로 저장되고, 다시 로그인해도 유지되며, 로그인·내 정보 응답에 settings가 들어 있음")
    void perUserAndInUserResponse() throws Exception {
        String a = signupAndGetToken("a@example.com", "music1234", "에이유저");
        String b = signupAndGetToken("b@example.com", "music1234", "비유저");
        putSettings(a, "{\"themeMode\":\"DARK\",\"textScale\":1.25}").andExpect(status().isOk());

        getJson("/api/users/me/settings", b)
                .andExpect(jsonPath("$.themeMode").value("SYSTEM"))
                .andExpect(jsonPath("$.textScale").value(1.0));

        var login = postJson("/api/auth/login", null, "{\"email\":\"a@example.com\",\"password\":\"music1234\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.settings.themeMode").value("DARK"))
                .andExpect(jsonPath("$.user.settings.textScale").value(1.25));
        String token = read(login, "$.accessToken");

        getJson("/api/users/me", token)
                .andExpect(jsonPath("$.settings.themeMode").value("DARK"))
                .andExpect(jsonPath("$.settings.textScale").value(1.25));

        // 가입 직후 응답에도 기본 설정이 들어 있음
        postJson("/api/auth/signup", null, "{\"email\":\"c@example.com\",\"password\":\"music1234\",\"nickname\":\"씨유저\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.settings.themeMode").value("SYSTEM"))
                .andExpect(jsonPath("$.user.settings.textScale").value(1.0));
    }

    @Test
    @DisplayName("다른 사람에게 보이는 프로필에는 settings가 없음")
    void notExposedToOthers() throws Exception {
        String me = signupAndGetToken("나야나");
        long myId = ((Number) read(getJson("/api/users/me", me), "$.id")).longValue();
        String other = signupAndGetToken("구경꾼");
        getJson("/api/users/" + myId + "/reviews", other)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.settings").doesNotExist());
    }

    @Test
    @DisplayName("로그인하지 않으면 401")
    void requiresLogin() throws Exception {
        getJson("/api/users/me/settings", null).andExpect(status().isUnauthorized());
        putSettings(null, "{\"themeMode\":\"DARK\"}").andExpect(status().isUnauthorized());
    }
}
