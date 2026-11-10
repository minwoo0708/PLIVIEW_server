package com.musicreview.server.feed;

import com.musicreview.server.feed.dto.PopularItemResponse;
import com.musicreview.server.review.domain.PopularItemRow;
import com.musicreview.server.review.domain.RatingMath;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 집계 행 → 화면용 응답. 표시 정보(이름·이미지)는 그 항목의 가장 최근 리뷰에서 가져옴 */
@Component
@RequiredArgsConstructor
public class PopularItemMapper {

    private final ReviewRepository reviewRepository;

    public PopularItemResponse toResponse(PopularItemRow row) {
        ReviewResponse.Item item = reviewRepository
                .findFirstByTypeAndSpotifyIdOrderByCreatedAtDescIdDesc(row.type(), row.spotifyId())
                .map(review -> new ReviewResponse.Item(
                        review.getItemName(), review.getItemSubtitle(), review.getItemImageUrl()))
                .orElse(null);
        return new PopularItemResponse(row.type(), row.spotifyId(), item,
                row.reviewCount(), RatingMath.round(row.averageRating()));
    }
}
