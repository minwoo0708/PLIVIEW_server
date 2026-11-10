package com.musicreview.server.user;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.spotify.SpotifyClient;
import com.musicreview.server.user.domain.User;
import com.musicreview.server.user.domain.UserRepository;
import com.musicreview.server.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PLIVIEW 계정 ↔ Spotify 계정 연동.
 * 연동한 사용자만 베스트 큐레이터가 될 수 있습니다(플리 owner.id = spotifyUserId).
 */
@Service
@RequiredArgsConstructor
public class SpotifyLinkService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final SpotifyClient spotifyClient;

    @Transactional
    public UserResponse link(Long userId, String spotifyAccessToken) {
        User user = userService.getUser(userId);
        SpotifyClient.SpotifyUserProfile profile = spotifyClient.getCurrentUser(spotifyAccessToken.strip());

        userRepository.findBySpotifyUserId(profile.id())
                .filter(other -> !other.getId().equals(userId))
                .ifPresent(other -> {
                    throw new BusinessException(ErrorCode.SPOTIFY_ALREADY_LINKED);
                });

        String displayName = profile.displayName() == null || profile.displayName().isBlank()
                ? profile.id()
                : truncate(profile.displayName().strip(), 100);
        user.linkSpotify(profile.id(), displayName);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 Spotify 계정을 두 PLIVIEW 계정에 연동한 경우 (UNIQUE 제약)
            throw new BusinessException(ErrorCode.SPOTIFY_ALREADY_LINKED);
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse unlink(Long userId) {
        User user = userService.getUser(userId);
        user.unlinkSpotify();
        return UserResponse.from(user);
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
