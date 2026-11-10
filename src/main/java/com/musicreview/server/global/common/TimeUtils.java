package com.musicreview.server.global.common;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 서버 시간대는 Asia/Seoul로 고정합니다. DB에는 LocalDateTime(KST), 응답에는 +09:00 오프셋을 붙입니다.
 */
public final class TimeUtils {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private TimeUtils() {
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now(ZONE);
    }

    public static OffsetDateTime toOffset(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toOffsetDateTime();
    }
}
