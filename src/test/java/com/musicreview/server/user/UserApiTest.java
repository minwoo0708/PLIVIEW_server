package com.musicreview.server.user;

import com.musicreview.server.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserApiTest extends ApiTestSupport {

    @Test
    @DisplayName("닉네임·프로필 사진 수정, 빈 문자열이면 사진 삭제")
    void updateMe() throws Exception {
        String token = signupAndGetToken("처음");
        patchJson("/api/users/me", token, """
                {"nickname":"  바꾼닉  ","profileImageUrl":"https://example.com/me.png"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("바꾼닉"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://example.com/me.png"));

        patchJson("/api/users/me", token, "{\"profileImageUrl\":\"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("바꾼닉"))
                .andExpect(jsonPath("$.profileImageUrl").doesNotExist());

        patchJson("/api/users/me", token, "{\"profileImageUrl\":\"javascript:alert(1)\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비밀번호가 틀리면 400, 바꾸면 새 비밀번호로만 로그인")
    void changePassword() throws Exception {
        String token = signupAndGetToken("pw@example.com", "music1234", "비번");

        patchJson("/api/users/me/password", token, """
                {"currentPassword":"wrong1234","newPassword":"newpass5678"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));

        patchJson("/api/users/me/password", token, """
                {"currentPassword":"music1234","newPassword":"newpass5678"}
                """)
                .andExpect(status().isNoContent());

        postJson("/api/auth/login", null, "{\"email\":\"pw@example.com\",\"password\":\"music1234\"}")
                .andExpect(status().isUnauthorized());
        postJson("/api/auth/login", null, "{\"email\":\"pw@example.com\",\"password\":\"newpass5678\"}")
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("탈퇴: 비밀번호 확인 후 계정과 리뷰 삭제, 남은 토큰은 404")
    void withdraw() throws Exception {
        String token = signupAndGetToken("bye@example.com", "music1234", "탈퇴");
        Number reviewId = read(postJson("/api/reviews", token, """
                {"type":"TRACK","spotifyId":"t1","item":{"name":"곡"},"rating":3.0,"tags":["태그"]}
                """).andExpect(status().isCreated()), "$.id");
        String other = signupAndGetToken("남은사람");

        deleteJson("/api/users/me", token, "{\"password\":\"wrong1234\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));

        deleteJson("/api/users/me", token, "{\"password\":\"music1234\"}")
                .andExpect(status().isNoContent());

        getJson("/api/users/me", token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        getJson("/api/reviews/" + reviewId, other).andExpect(status().isNotFound());
        postJson("/api/auth/login", null, "{\"email\":\"bye@example.com\",\"password\":\"music1234\"}")
                .andExpect(status().isUnauthorized());
    }
}
