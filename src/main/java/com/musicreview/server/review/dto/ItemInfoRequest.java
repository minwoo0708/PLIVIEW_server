package com.musicreview.server.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 리뷰 대상의 표시용 정보 (앱이 Spotify에서 받은 값을 그대로 전달) */
public record ItemInfoRequest(
        @NotBlank(message = "항목 이름을 입력해 주세요.")
        @Size(max = 200, message = "항목 이름은 200자 이하여야 합니다.")
        String name,

        @Size(max = 200, message = "부제목은 200자 이하여야 합니다.")
        String subtitle,

        @Size(max = 500, message = "이미지 주소는 500자 이하여야 합니다.")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "http 또는 https 주소여야 합니다.")
        String imageUrl
) {
}
