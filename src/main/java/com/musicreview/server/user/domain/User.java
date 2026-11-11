package com.musicreview.server.user.domain;

import com.musicreview.server.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_users_spotify_user_id", columnNames = "spotify_user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소문자로 정규화해서 저장 */
    @Column(nullable = false, length = 100)
    private String email;

    /** BCrypt 해시. 절대 응답에 넣지 않음 */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /** 연동한 Spotify 계정 id (= 플리의 owner.id). 연동 안 했으면 null */
    @Column(name = "spotify_user_id", length = 64)
    private String spotifyUserId;

    @Column(name = "spotify_display_name", length = 100)
    private String spotifyDisplayName;

    /** 앱 화면 모드 설정 (기본: 기기 설정을 따름) */
    @Enumerated(EnumType.STRING)
    @Column(name = "theme_mode", nullable = false, length = 10)
    private ThemeMode themeMode = ThemeMode.SYSTEM;

    /** 글자 크기 배율 (0.80 ~ 1.50, 기본 1.00) */
    @Column(name = "text_scale", nullable = false, precision = 3, scale = 2)
    private BigDecimal textScale = DEFAULT_TEXT_SCALE;

    public static final BigDecimal DEFAULT_TEXT_SCALE = new BigDecimal("1.00");
    public static final BigDecimal MIN_TEXT_SCALE = new BigDecimal("0.80");
    public static final BigDecimal MAX_TEXT_SCALE = new BigDecimal("1.50");

    private User(String email, String passwordHash, String nickname) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
    }

    public static User create(String email, String passwordHash, String nickname) {
        return new User(email, passwordHash, nickname);
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    /** null 또는 빈 문자열이면 사진 삭제 */
    public void changeProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = (profileImageUrl == null || profileImageUrl.isBlank()) ? null : profileImageUrl;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** null인 값은 그대로 둠. textScale은 소수 둘째 자리로 반올림해서 저장 */
    public void changeSettings(ThemeMode themeMode, BigDecimal textScale) {
        if (themeMode != null) {
            this.themeMode = themeMode;
        }
        if (textScale != null) {
            this.textScale = textScale.setScale(2, RoundingMode.HALF_UP);
        }
    }

    public boolean isSpotifyLinked() {
        return spotifyUserId != null;
    }

    public void linkSpotify(String spotifyUserId, String displayName) {
        this.spotifyUserId = spotifyUserId;
        this.spotifyDisplayName = displayName;
    }

    public void unlinkSpotify() {
        this.spotifyUserId = null;
        this.spotifyDisplayName = null;
    }
}
