package com.musicreview.server.feed;

import com.musicreview.server.feed.dto.PopularItemResponse;
import com.musicreview.server.feed.dto.WeeklyPlaylistResponse;
import com.musicreview.server.global.common.PageResponse;
import com.musicreview.server.global.security.CurrentUserId;
import com.musicreview.server.review.dto.ReviewResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 홈 화면: 최신 리뷰와 인기 항목 */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public PageResponse<ReviewResponse> feed(
            @CurrentUserId Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~50입니다.")
            @Max(value = 50, message = "size는 1~50입니다.") int size) {
        return feedService.getFeed(userId, type, page, size);
    }

    @GetMapping("/popular")
    public List<PopularItemResponse> popular(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "days는 1~90입니다.")
            @Max(value = 90, message = "days는 1~90입니다.") int days,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit은 1~50입니다.")
            @Max(value = 50, message = "limit은 1~50입니다.") int limit) {
        return feedService.getPopular(type, days, limit);
    }

    /** 홈 "이번 주의 플리": 리뷰가 많이(기본 50개 이상) 쌓인 플리 중 평점 높은 순 */
    @GetMapping("/weekly-playlists")
    public List<WeeklyPlaylistResponse> weeklyPlaylists(
            @RequestParam(defaultValue = "50") @Min(value = 1, message = "minReviews는 1~1000입니다.")
            @Max(value = 1000, message = "minReviews는 1~1000입니다.") int minReviews,
            @RequestParam(required = false) @Min(value = 1, message = "days는 1~365입니다.")
            @Max(value = 365, message = "days는 1~365입니다.") Integer days,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit은 1~50입니다.")
            @Max(value = 50, message = "limit은 1~50입니다.") int limit) {
        return feedService.getWeeklyPlaylists(minReviews, days, limit);
    }
}
