package com.musicreview.server.user.dto;

import com.musicreview.server.global.validation.Password;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChangePasswordRequest(
        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword,

        @NotNull(message = "새 비밀번호를 입력해 주세요.")
        @Password
        String newPassword
) {
}
