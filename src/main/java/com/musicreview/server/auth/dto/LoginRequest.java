package com.musicreview.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 로그인은 형식 검사를 최소로 하고, 틀리면 LOGIN_FAILED 하나로 응답 */
public record LoginRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {

    public LoginRequest {
        email = email == null ? null : email.strip();
    }
}
