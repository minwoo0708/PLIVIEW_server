package com.musicreview.server.review;

import com.musicreview.server.global.security.CurrentUserId;
import com.musicreview.server.review.dto.CreateReviewRequest;
import com.musicreview.server.review.dto.ReviewResponse;
import com.musicreview.server.review.dto.UpdateReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(@CurrentUserId Long userId,
                                                 @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.create(userId, request);
        return ResponseEntity.created(URI.create("/api/reviews/" + response.id())).body(response);
    }

    /** /mine 이 /{reviewId} 보다 먼저 매칭됨 (리터럴 경로 우선) */
    @GetMapping("/mine")
    public List<ReviewResponse> mine(@CurrentUserId Long userId,
                                     @RequestParam String type,
                                     @RequestParam List<String> spotifyIds) {
        return reviewService.findMine(userId, type, spotifyIds);
    }

    @GetMapping("/{reviewId}")
    public ReviewResponse get(@CurrentUserId Long userId, @PathVariable Long reviewId) {
        return reviewService.get(userId, reviewId);
    }

    @PatchMapping("/{reviewId}")
    public ReviewResponse update(@CurrentUserId Long userId, @PathVariable Long reviewId,
                                 @Valid @RequestBody UpdateReviewRequest request) {
        return reviewService.update(userId, reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long userId, @PathVariable Long reviewId) {
        reviewService.delete(userId, reviewId);
    }
}
