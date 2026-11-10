package com.musicreview.server.auth.dto;

import com.musicreview.server.global.validation.Nickname;
import com.musicreview.server.global.validation.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 아닙니다.")
        @Size(max = 100, message = "이메일은 100자 이하로 입력해 주세요.")
        String email,

        @NotNull(message = "비밀번호를 입력해 주세요.")
        @Password
        String password,

        @NotNull(message = "닉네임을 입력해 주세요.")
        @Nickname
        String nickname
) {

    /** 앱 입력창에 남은 앞뒤 공백 때문에 이메일 형식 검사가 실패하지 않도록 먼저 제거 */
    public SignupRequest {
        email = email == null ? null : email.strip();
    }
}
