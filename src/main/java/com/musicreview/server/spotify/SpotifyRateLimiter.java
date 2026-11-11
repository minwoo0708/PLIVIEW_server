package com.musicreview.server.spotify;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 사용자별 Spotify 대리 조회 요청 제한 (1분 고정 창).
 * 한 사람이 검색을 연타해서 공용 앱 토큰의 Spotify 한도를 다 쓰는 것을 막습니다.
 * 서버가 한 대일 때 기준의 메모리 카운터입니다(여러 대로 늘리면 Redis 등으로 바꿔야 함).
 */
@Component
public class SpotifyRateLimiter {

    private final int limitPerMinute;
    private final Clock clock;
    private final Cache<String, AtomicInteger> counters = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .build();

    @Autowired
    public SpotifyRateLimiter(SpotifyProperties properties) {
        this(properties.requestsPerMinutePerUser(), Clock.systemUTC());
    }

    SpotifyRateLimiter(int limitPerMinute, Clock clock) {
        this.limitPerMinute = limitPerMinute;
        this.clock = clock;
    }

    /** 한도를 넘으면 429 TOO_MANY_REQUESTS */
    public void check(Long userId) {
        long minute = clock.millis() / 60_000;
        AtomicInteger counter = counters.get(userId + ":" + minute, key -> new AtomicInteger());
        if (counter.incrementAndGet() > limitPerMinute) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }
}
