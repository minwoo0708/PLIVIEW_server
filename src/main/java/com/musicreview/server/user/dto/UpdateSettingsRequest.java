package com.musicreview.server.user.dto;

import com.musicreview.server.user.domain.ThemeMode;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/**
 * 보낸 항목만 바꿉니다 (둘 다 선택).
 * themeMode는 SYSTEM / LIGHT / DARK만 허용(그 밖의 값은 400), textScale은 0.8 ~ 1.5.
 */
public record UpdateSettingsRequest(
        ThemeMode themeMode,

        @DecimalMin(value = "0.80", message = "글자 크기 배율은 0.8 이상이어야 합니다.")
        @DecimalMax(value = "1.50", message = "글자 크기 배율은 1.5 이하여야 합니다.")
        BigDecimal textScale
) {
}
