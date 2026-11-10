package com.musicreview.server.review.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 평균 반올림, 별점 분포 계산 (항목 요약, 유저 통계, 랭킹이 같이 씀) */
public final class RatingMath {

    private RatingMath() {
    }

    /** 소수 첫째 자리 반올림, 값이 없으면 null */
    public static BigDecimal round(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * "0.5" ~ "5.0" 10칸을 0으로 채운 뒤 [rating, count] 행으로 덮어씀.
     * @param ratingCounts ReviewRepository의 group by rating 결과
     */
    public static Map<String, Long> distribution(List<Object[]> ratingCounts) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (int half = 1; half <= 10; half++) {
            distribution.put(BigDecimal.valueOf(half * 5L, 1).toPlainString(), 0L); // 0.5, 1.0, ... 5.0
        }
        for (Object[] row : ratingCounts) {
            String key = ((BigDecimal) row[0]).setScale(1, RoundingMode.UNNECESSARY).toPlainString();
            distribution.put(key, ((Number) row[1]).longValue());
        }
        return distribution;
    }
}
