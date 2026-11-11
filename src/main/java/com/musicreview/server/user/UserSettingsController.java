package com.musicreview.server.user;

import com.musicreview.server.global.security.CurrentUserId;
import com.musicreview.server.user.dto.UpdateSettingsRequest;
import com.musicreview.server.user.dto.UserSettingsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 앱 화면 설정(다크모드, 글자 크기)을 계정에 저장해서 기기를 바꿔도 이어지게 함 */
@RestController
@RequestMapping("/api/users/me/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserService userService;

    @GetMapping
    public UserSettingsResponse get(@CurrentUserId Long userId) {
        return userService.getSettings(userId);
    }

    /** PUT이지만 보낸 항목만 바꿉니다 (둘 다 선택) */
    @PutMapping
    public UserSettingsResponse update(@CurrentUserId Long userId, @Valid @RequestBody UpdateSettingsRequest request) {
        return userService.updateSettings(userId, request);
    }
}
