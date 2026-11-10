package com.musicreview.server.user.dto;

import com.musicreview.server.global.validation.Nickname;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 보낸 필드만 변경. profileImageUrl을 ""로 보내면 사진 삭제 */
public record UpdateMeRequest(
        @Nickname
        String nickname,

        @Size(max = 500, message = "프로필 사진 주소는 500자 이하로 입력해 주세요.")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "http 또는 https 주소여야 합니다.")
        String profileImageUrl
) {
}
