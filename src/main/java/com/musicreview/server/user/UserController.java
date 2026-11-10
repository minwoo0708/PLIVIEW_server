package com.musicreview.server.user;

import com.musicreview.server.global.security.CurrentUserId;
import com.musicreview.server.user.dto.ChangePasswordRequest;
import com.musicreview.server.user.dto.LinkSpotifyRequest;
import com.musicreview.server.user.dto.UpdateMeRequest;
import com.musicreview.server.user.dto.UserResponse;
import com.musicreview.server.user.dto.WithdrawRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SpotifyLinkService spotifyLinkService;

    @GetMapping
    public UserResponse me(@CurrentUserId Long userId) {
        return userService.getMe(userId);
    }

    @PatchMapping
    public UserResponse updateMe(@CurrentUserId Long userId, @Valid @RequestBody UpdateMeRequest request) {
        return userService.updateMe(userId, request);
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@CurrentUserId Long userId, @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
    }

    /** Spotify 계정 연동: 앱이 PKCE로 받은 토큰을 보내면 서버가 /v1/me 로 확인 */
    @PutMapping("/spotify")
    public UserResponse linkSpotify(@CurrentUserId Long userId, @Valid @RequestBody LinkSpotifyRequest request) {
        return spotifyLinkService.link(userId, request.spotifyAccessToken());
    }

    @DeleteMapping("/spotify")
    public UserResponse unlinkSpotify(@CurrentUserId Long userId) {
        return spotifyLinkService.unlink(userId);
    }

    /** 회원 탈퇴: 본문에 비밀번호 { "password": "..." } */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@CurrentUserId Long userId, @Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(userId, request);
    }
}
