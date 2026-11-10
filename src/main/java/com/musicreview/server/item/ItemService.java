package com.musicreview.server.item;

import com.musicreview.server.global.common.PageResponse;
import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.item.dto.ItemSummaryResponse;
import com.musicreview.server.review.domain.ItemStats;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.RatingMath;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {

    private final ReviewRepository reviewRepository;

    public PageResponse<ReviewResponse> getReviews(Long userId, String type, String spotifyId,
                                                   int page, int size, String sort) {
        ItemType itemType = ItemType.from(type);
        PageRequest pageable = PageRequest.of(page, size, toSort(sort));
        return PageResponse.of(
                reviewRepository.findByTypeAndSpotifyId(itemType, spotifyId, pageable),
                review -> ReviewResponse.of(review, userId));
    }

    public ItemSummaryResponse getSummary(Long userId, String type, String spotifyId) {
        ItemType itemType = ItemType.from(type);
        ItemStats stats = reviewRepository.findItemStats(itemType, spotifyId);
        long count = stats.reviewCount() == null ? 0 : stats.reviewCount();

        ItemSummaryResponse.PlaylistAverages playlistAverages = null;
        if (itemType == ItemType.PLAYLIST && count > 0) {
            playlistAverages = new ItemSummaryResponse.PlaylistAverages(
                    RatingMath.round(stats.averageConcept()),
                    RatingMath.round(stats.averageFlow()),
                    RatingMath.round(stats.averageDiscovery()));
        }

        ReviewResponse myReview = reviewRepository.findByUserIdAndTypeAndSpotifyId(userId, itemType, spotifyId)
                .map(review -> ReviewResponse.of(review, userId))
                .orElse(null);

        return new ItemSummaryResponse(itemType, spotifyId, count, RatingMath.round(stats.averageRating()),
                RatingMath.distribution(reviewRepository.countByRating(itemType, spotifyId)),
                playlistAverages, myReview);
    }

    private static Sort toSort(String sort) {
        Sort latest = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return switch (sort) {
            case "latest" -> latest;
            case "rating_desc" -> Sort.by(Sort.Order.desc("rating")).and(latest);
            case "rating_asc" -> Sort.by(Sort.Order.asc("rating")).and(latest);
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "sort는 latest, rating_desc, rating_asc 중 하나여야 합니다.");
        };
    }
}
