package com.musicreview.server.user;

import com.musicreview.server.global.common.PageResponse;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.RatingMath;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.review.dto.ReviewResponse;
import com.musicreview.server.user.domain.User;
import com.musicreview.server.user.dto.UserReviewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 유저 프로필: 리뷰 목록 + 별점 분포 등 통계 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReviewService {

    private final UserService userService;
    private final ReviewRepository reviewRepository;

    /**
     * @param type      null이면 전체 타입
     * @param minRating null이면 조건 없음 (예: 4.5 → 리뷰 기반 플리 생성용 고득점 곡)
     */
    public UserReviewsResponse getUserReviews(Long viewerId, Long userId, String type, BigDecimal minRating,
                                              int page, int size) {
        User user = userService.getUser(userId);
        ItemType itemType = type == null ? null : ItemType.from(type);
        BigDecimal min = minRating == null ? null : minRating.setScale(1, RoundingMode.UNNECESSARY);

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        PageResponse<ReviewResponse> reviews = PageResponse.of(
                reviewRepository.findUserReviews(userId, itemType, min, pageable),
                review -> ReviewResponse.of(review, viewerId));

        return new UserReviewsResponse(UserReviewsResponse.PublicUser.from(user), stats(userId), reviews);
    }

    private UserReviewsResponse.Stats stats(Long userId) {
        Object[] totals = reviewRepository.findUserTotals(userId).getFirst();
        long count = totals[0] == null ? 0 : ((Number) totals[0]).longValue();
        BigDecimal average = RatingMath.round(totals[1] == null ? null : ((Number) totals[1]).doubleValue());

        Map<String, Long> countByType = new LinkedHashMap<>();
        for (ItemType itemType : ItemType.values()) {
            countByType.put(itemType.name(), 0L);
        }
        List<Object[]> byType = reviewRepository.countUserReviewsByType(userId);
        for (Object[] row : byType) {
            countByType.put(((ItemType) row[0]).name(), ((Number) row[1]).longValue());
        }

        return new UserReviewsResponse.Stats(count, average,
                RatingMath.distribution(reviewRepository.countUserReviewsByRating(userId)), countByType);
    }
}
