package com.musicreview.server.user.dto;

import com.musicreview.server.global.common.PageResponse;
import com.musicreview.server.global.common.TimeUtils;
import com.musicreview.server.review.dto.ReviewResponse;
import com.musicreview.server.user.domain.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/** docs/API.md 21. 유저 프로필 화면: 공개 정보 + 전체 통계 + 리뷰 목록 */
public record UserReviewsResponse(
        PublicUser user,
        Stats stats,
        PageResponse<ReviewResponse> reviews
) {

    /** 다른 사람에게 보이는 정보 (이메일 없음) */
    public record PublicUser(Long id, String nickname, String profileImageUrl, OffsetDateTime createdAt) {

        public static PublicUser from(User user) {
            return new PublicUser(user.getId(), user.getNickname(), user.getProfileImageUrl(),
                    TimeUtils.toOffset(user.getCreatedAt()));
        }
    }

    /** 필터(type, minRating)와 상관없이 그 유저의 전체 리뷰 기준 */
    public record Stats(
            long reviewCount,
            BigDecimal averageRating,
            Map<String, Long> ratingDistribution,
            Map<String, Long> countByType
    ) {
    }
}
