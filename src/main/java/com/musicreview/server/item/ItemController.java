package com.musicreview.server.item;

import com.musicreview.server.global.common.PageResponse;
import com.musicreview.server.global.security.CurrentUserId;
import com.musicreview.server.global.validation.SpotifyId;
import com.musicreview.server.item.dto.ItemSummaryResponse;
import com.musicreview.server.review.dto.ReviewResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 곡·앨범·플리 상세 화면용: 리뷰 목록과 요약 */
@RestController
@RequestMapping("/api/items/{type}/{spotifyId}")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/reviews")
    public PageResponse<ReviewResponse> reviews(
            @CurrentUserId Long userId,
            @PathVariable String type,
            @PathVariable @SpotifyId String spotifyId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~50입니다.")
            @Max(value = 50, message = "size는 1~50입니다.") int size,
            @RequestParam(defaultValue = "latest") String sort) {
        return itemService.getReviews(userId, type, spotifyId, page, size, sort);
    }

    @GetMapping("/summary")
    public ItemSummaryResponse summary(@CurrentUserId Long userId,
                                       @PathVariable String type,
                                       @PathVariable @SpotifyId String spotifyId) {
        return itemService.getSummary(userId, type, spotifyId);
    }
}
