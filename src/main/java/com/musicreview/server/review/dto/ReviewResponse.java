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
        return new ReviewResponse(
                review.getId(),
                review.getType(),
                review.getSpotifyId(),
                new Item(review.getItemName(), review.getItemSubtitle(), review.getItemImageUrl()),
                review.getRating(),
                review.getComment(),
                review.getTags().stream().sorted().toList(),
                playlistScores,
                Author.from(review.getUser()),
                review.isWrittenBy(viewerId),
                TimeUtils.toOffset(review.getCreatedAt()),
                TimeUtils.toOffset(review.getUpdatedAt())
        );
    }
}
