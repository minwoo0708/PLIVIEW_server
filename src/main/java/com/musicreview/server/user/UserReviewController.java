package com.musicreview.server.user;

import com.musicreview.server.global.security.CurrentUserId;
import com.musicreview.server.global.validation.Score;
import com.musicreview.server.user.dto.UserReviewsResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/users/{userId}/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final UserReviewService userReviewService;

    /** 프로필 화면. 내 프로필은 GET /api/users/me 의 id를 넣어 호출 */
    @GetMapping
    public UserReviewsResponse reviews(
            @CurrentUserId Long viewerId,
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @Score BigDecimal minRating,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~50입니다.")
            @Max(value = 50, message = "size는 1~50입니다.") int size) {
        return userReviewService.getUserReviews(viewerId, userId, type, minRating, page, size);
    }
}
