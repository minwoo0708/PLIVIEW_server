package com.musicreview.server.feed;

import com.musicreview.server.feed.dto.PopularItemResponse;
import com.musicreview.server.global.common.PageResponse;
import com.musicreview.server.global.common.TimeUtils;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.PopularItemRow;
import com.musicreview.server.review.domain.RatingMath;
import com.musicreview.server.review.domain.Review;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final ReviewRepository reviewRepository;

    /** 최신 리뷰 피드. type이 null이면 전체 */
    public PageResponse<ReviewResponse> getFeed(Long userId, String type, int page, int size) {
        ItemType itemType = type == null ? null : ItemType.from(type);
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.of(reviewRepository.findFeed(itemType, pageable),
                review -> ReviewResponse.of(review, userId));
    }

    /** 최근 days일 동안 리뷰가 많이 달린 항목 */
    public List<PopularItemResponse> getPopular(String type, int days, int limit) {
        ItemType itemType = type == null ? null : ItemType.from(type);
        LocalDateTime since = TimeUtils.now().toLocalDateTime().minusDays(days);
        List<PopularItemRow> rows = reviewRepository.findPopularItems(itemType, since, PageRequest.of(0, limit));
        // 항목 수가 최대 50개라 항목마다 최근 리뷰 1건을 조회해 표시용 정보를 채움
        return rows.stream().map(this::toResponse).toList();
    }

    private PopularItemResponse toResponse(PopularItemRow row) {
        ReviewResponse.Item item = reviewRepository
                .findFirstByTypeAndSpotifyIdOrderByCreatedAtDescIdDesc(row.type(), row.spotifyId())
                .map(FeedService::toItem)
                .orElse(null);
        return new PopularItemResponse(row.type(), row.spotifyId(), item,
                row.reviewCount(), RatingMath.round(row.averageRating()));
    }

    private static ReviewResponse.Item toItem(Review review) {
        return new ReviewResponse.Item(review.getItemName(), review.getItemSubtitle(), review.getItemImageUrl());
    }
}
