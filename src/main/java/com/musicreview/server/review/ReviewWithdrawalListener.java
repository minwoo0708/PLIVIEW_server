package com.musicreview.server.review;

import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.user.UserWithdrawnEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 회원 탈퇴 시 그 사용자가 쓴 리뷰(태그 포함)를 지운다 (탈퇴 트랜잭션에 함께 묶임) */
@Component
@RequiredArgsConstructor
class ReviewWithdrawalListener {

    private final ReviewRepository reviewRepository;

    @EventListener
    void onUserWithdrawn(UserWithdrawnEvent event) {
        reviewRepository.deleteAll(reviewRepository.findByUserId(event.userId()));
    }
}
