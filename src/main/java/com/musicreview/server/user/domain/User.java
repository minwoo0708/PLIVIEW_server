package com.musicreview.server.user.domain;

import com.musicreview.server.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
