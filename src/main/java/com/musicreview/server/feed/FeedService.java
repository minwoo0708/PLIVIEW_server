package com.musicreview.server.feed;

import com.musicreview.server.feed.dto.PopularItemResponse;
import com.musicreview.server.feed.dto.WeeklyPlaylistResponse;
import com.musicreview.server.global.common.PageResponse;
import com.musicreview.server.global.common.TimeUtils;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.PopularItemRow;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    /** days를 안 주면 전체 기간 */
    private static final LocalDateTime ALL_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final ReviewRepository reviewRepository;
    private final PopularItemMapper popularItemMapper;

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
        return rows.stream().map(popularItemMapper::toResponse).toList();
    }

    /**
     * 이번 주의 플리: 리뷰가 minReviews(기본 50) 이상인 플리를 평균 총점 순으로.
     * @param days null이면 전체 기간의 리뷰로 계산
     */
    public List<WeeklyPlaylistResponse> getWeeklyPlaylists(int minReviews, Integer days, int limit) {
        LocalDateTime since = days == null ? ALL_TIME : TimeUtils.now().toLocalDateTime().minusDays(days);
        List<PopularItemRow> rows = reviewRepository.findTopRatedPlaylists(since, minReviews, PageRequest.of(0, limit));
        List<WeeklyPlaylistResponse> result = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            PopularItemResponse item = popularItemMapper.toResponse(rows.get(i));
            result.add(new WeeklyPlaylistResponse(i + 1, item.spotifyId(), item.item(),
                    item.reviewCount(), item.averageRating()));
        }
        return result;
    }
}
