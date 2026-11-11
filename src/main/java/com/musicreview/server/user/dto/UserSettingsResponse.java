package com.musicreview.server.user.dto;

import com.musicreview.server.user.domain.ThemeMode;
import com.musicreview.server.user.domain.User;

import java.math.BigDecimal;

/** 앱 화면 설정. 한 번도 저장하지 않았으면 SYSTEM, 1.00 */
public record UserSettingsResponse(
        ThemeMode themeMode,
        BigDecimal textScale
) {

    public static UserSettingsResponse from(User user) {
        return new UserSettingsResponse(user.getThemeMode(), user.getTextScale());
    }
}
