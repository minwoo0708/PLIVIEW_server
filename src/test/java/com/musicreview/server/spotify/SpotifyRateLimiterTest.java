package com.musicreview.server.spotify;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpotifyRateLimiterTest {

    /** 테스트에서 시간을 직접 돌리는 시계 */
    static class MutableClock extends Clock {
        final AtomicLong millis = new AtomicLong(0);

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
        @Override public long millis() { return millis.get(); }
    }

    @Test
    @DisplayName("1분에 한도만큼만 허용, 넘으면 429, 다음 분에는 다시 허용, 사용자별로 따로 셈")
    void limitPerUserPerMinute() {
        MutableClock clock = new MutableClock();
        SpotifyRateLimiter limiter = new SpotifyRateLimiter(3, clock);

        limiter.check(1L);
        limiter.check(1L);
        limiter.check(1L);
        assertThatThrownBy(() -> limiter.check(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);

        // 다른 사용자는 영향 없음
        limiter.check(2L);

        // 다음 분
        clock.millis.set(60_000);
        limiter.check(1L);
        assertThat(true).isTrue();
    }
}
