package com.musicreview.server.review.dto;

import com.musicreview.server.global.common.TimeUtils;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.Review;
import com.musicreview.server.user.domain.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** docs/API.md 3.2 */
public record ReviewResponse(
        Long id,
        ItemType type,
        String spotifyId,
        Item item,
        BigDecimal rating,
        String comment,
        List<String> tags,
        PlaylistScores playlistScores,
        Highlights highlights,
        Author author,
        boolean isMine,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public record Item(String name, String subtitle, String imageUrl) {
    }

    public record PlaylistScores(
            BigDecimal concept,
            BigDecimal flow,
            BigDecimal discovery,
            String snapshotId,
            String ownerSpotifyId
    ) {
    }

    public record TrackRef(String spotifyId, String name) {

        static TrackRef of(String spotifyId, String name) {
            return spotifyId == null ? null : new TrackRef(spotifyId, name);
        }
    }

    /** 앨범·플리 리뷰 하이라이트. 곡 리뷰면 highlights 자체가 null */
    public record Highlights(
            TrackRef bestTrack,
            TrackRef worstTrack,
            TrackRef underratedTrack,
            String bestMoment,
            String body
    ) {
    }

    /** 다른 사람에게 보이는 작성자 정보 (이메일 없음) */
    public record Author(Long id, String nickname, String profileImageUrl) {

        static Author from(User user) {
            return new Author(user.getId(), user.getNickname(), user.getProfileImageUrl());
        }
    }

    /** @param viewerId 요청한 사용자 id (isMine 계산용) */
    public static ReviewResponse of(Review review, Long viewerId) {
        PlaylistScores playlistScores = review.isPlaylist()
                ? new PlaylistScores(review.getConceptScore(), review.getFlowScore(), review.getDiscoveryScore(),
                review.getSnapshotId(), review.getOwnerSpotifyId())
                : null;
        Highlights highlights = review.isTrack()
                ? null
                : new Highlights(
                TrackRef.of(review.getBestTrackId(), review.getBestTrackName()),
                TrackRef.of(review.getWorstTrackId(), review.getWorstTrackName()),
                TrackRef.of(review.getUnderratedTrackId(), review.getUnderratedTrackName()),
                review.getBestMoment(),
                review.getBody());
        return new ReviewResponse(
                review.getId(),
                review.getType(),
                review.getSpotifyId(),
                new Item(review.getItemName(), review.getItemSubtitle(), review.getItemImageUrl()),
                review.getRating(),
                review.getComment(),
                review.getTags().stream().sorted().toList(),
                playlistScores,
                highlights,
                Author.from(review.getUser()),
                review.isWrittenBy(viewerId),
                TimeUtils.toOffset(review.getCreatedAt()),
                TimeUtils.toOffset(review.getUpdatedAt())
        );
    }
}
