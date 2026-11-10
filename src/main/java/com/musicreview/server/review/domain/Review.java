package com.musicreview.server.review.domain;

import com.musicreview.server.global.common.BaseTimeEntity;
import com.musicreview.server.user.domain.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 곡·앨범·플리 리뷰. 사용자당 항목(type + spotifyId)마다 1개.
 * 플리 전용 컬럼(conceptScore 등)은 곡·앨범이면 null.
 */
@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(name = "uk_reviews_user_item", columnNames = {"user_id", "type", "spotify_id"}),
        indexes = {
                @Index(name = "idx_reviews_item", columnList = "type, spotify_id, created_at"),
                @Index(name = "idx_reviews_created", columnList = "created_at"),
                @Index(name = "idx_reviews_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_reviews_owner", columnList = "owner_spotify_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reviews_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemType type;

    @Column(name = "spotify_id", nullable = false, length = 64)
    private String spotifyId;

    // 표시용 정보 (작성 시 앱이 전달)
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_subtitle", length = 200)
    private String itemSubtitle;

    @Column(name = "item_image_url", length = 500)
    private String itemImageUrl;

    /** 0.5 ~ 5.0. 플리는 사용자가 매긴 총점 */
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(length = 200)
    private String comment;

    // 플레이리스트 전용
    @Column(name = "concept_score", precision = 2, scale = 1)
    private BigDecimal conceptScore;

    @Column(name = "flow_score", precision = 2, scale = 1)
    private BigDecimal flowScore;

    @Column(name = "discovery_score", precision = 2, scale = 1)
    private BigDecimal discoveryScore;

    @Column(name = "snapshot_id", length = 100)
    private String snapshotId;

    @Column(name = "owner_spotify_id", length = 64)
    private String ownerSpotifyId;

    // 앨범·플리 전용 하이라이트 (곡 리뷰는 null)
    @Column(name = "best_track_id", length = 64)
    private String bestTrackId;

    @Column(name = "best_track_name", length = 200)
    private String bestTrackName;

    @Column(name = "worst_track_id", length = 64)
    private String worstTrackId;

    @Column(name = "worst_track_name", length = 200)
    private String worstTrackName;

    /** 가장 과소평가된 곡: 앨범만 */
    @Column(name = "underrated_track_id", length = 64)
    private String underratedTrackId;

    @Column(name = "underrated_track_name", length = 200)
    private String underratedTrackName;

    /** 최고의 순간 (예: "2:31 브릿지에서 터지는 코러스") */
    @Column(name = "best_moment", length = 100)
    private String bestMoment;

    /** 전체 평가 (여러 줄) */
    @Column(name = "body", length = 2000)
    private String body;

    @ElementCollection
    @CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"),
            foreignKey = @ForeignKey(name = "fk_review_tags_review"))
    @Column(name = "tag", nullable = false, length = 20)
    private Set<String> tags = new HashSet<>();

    private Review(User user, ItemType type, String spotifyId) {
        this.user = user;
        this.type = type;
        this.spotifyId = spotifyId;
    }

    public static Review create(User user, ItemType type, String spotifyId) {
        return new Review(user, type, spotifyId);
    }

    public boolean isPlaylist() {
        return type == ItemType.PLAYLIST;
    }

    public boolean isWrittenBy(Long userId) {
        return user.getId().equals(userId);
    }

    public void changeItem(String name, String subtitle, String imageUrl) {
        this.itemName = name;
        this.itemSubtitle = blankToNull(subtitle);
        this.itemImageUrl = blankToNull(imageUrl);
    }

    public void changeRating(BigDecimal rating) {
        this.rating = rating;
    }

    /** 빈 문자열이면 한 줄 평 삭제 */
    public void changeComment(String comment) {
        this.comment = blankToNull(comment);
    }

    public void replaceTags(Collection<String> newTags) {
        this.tags.clear();
        this.tags.addAll(newTags);
    }

    public void changePlaylistScores(BigDecimal concept, BigDecimal flow, BigDecimal discovery) {
        if (concept != null) {
            this.conceptScore = concept;
        }
        if (flow != null) {
            this.flowScore = flow;
        }
        if (discovery != null) {
            this.discoveryScore = discovery;
        }
    }

    public void changeSnapshotId(String snapshotId) {
        this.snapshotId = blankToNull(snapshotId);
    }

    public void assignOwner(String ownerSpotifyId) {
        this.ownerSpotifyId = ownerSpotifyId;
    }

    public boolean isTrack() {
        return type == ItemType.TRACK;
    }

    /** null이면 그대로 둠 */
    public void changeBestTrack(String spotifyId, String name) {
        this.bestTrackId = spotifyId;
        this.bestTrackName = name;
    }

    public void changeWorstTrack(String spotifyId, String name) {
        this.worstTrackId = spotifyId;
        this.worstTrackName = name;
    }

    public void changeUnderratedTrack(String spotifyId, String name) {
        this.underratedTrackId = spotifyId;
        this.underratedTrackName = name;
    }

    /** 빈 문자열이면 삭제 */
    public void changeBestMoment(String bestMoment) {
        this.bestMoment = blankToNull(bestMoment);
    }

    /** 빈 문자열이면 삭제. 줄바꿈은 그대로 저장 */
    public void changeBody(String body) {
        this.body = (body == null || body.isBlank()) ? null : body.strip();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }
}
