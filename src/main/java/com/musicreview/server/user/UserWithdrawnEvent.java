package com.musicreview.server.user;

/**
 * 회원 탈퇴 시 발행되는 이벤트. 사용자가 쓴 데이터(리뷰 등)를 지우는 쪽이 이 이벤트를 받아 처리하므로,
 * user 패키지가 review 패키지를 몰라도 됩니다. 탈퇴 트랜잭션 안에서 동기로 실행됩니다.
 */
public record UserWithdrawnEvent(Long userId) {
}
