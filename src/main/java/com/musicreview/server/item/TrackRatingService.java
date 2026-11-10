package com.musicreview.server.item;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.global.validation.SpotifyId;
import com.musicreview.server.item.dto.TrackRatingResponse;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.RatingMath;
import com.musicreview.server.review.domain.Review;
import com.musicreview.server.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackRatingService {

    /** 플리 한 페이지(100곡)를 한 번에 조회할 수 있게 */
    static final int MAX_IDS = 100;

    private final ReviewRepository reviewRepository;

    /** 요청한 순서대로, 중복 제거해서 돌려줌 */
    public List<TrackRatingResponse> getRatings(Long userId, List<String> spotifyIds) {
        Set<String> ids = new LinkedHashSet<>();
        for (String id : spotifyIds) {
            String trimmed = id.strip();
            if (!trimmed.matches(SpotifyId.REGEX)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "spotifyIds에 올바르지 않은 id가 있습니다: " + trimmed);
            }
            ids.add(trimmed);
        }
        if (ids.isEmpty() || ids.size() > MAX_IDS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "spotifyIds는 1~100개까지 보낼 수 있습니다.");
        }

        Map<String, Object[]> stats = new HashMap<>();
        for (Object[] row : reviewRepository.findRatingsBySpotifyIds(ItemType.TRACK, ids)) {
            stats.put((String) row[0], row);
        }
        Map<String, BigDecimal> mine = new HashMap<>();
        for (Review review : reviewRepository.findByUserIdAndTypeAndSpotifyIdIn(userId, ItemType.TRACK, ids)) {
            mine.put(review.getSpotifyId(), review.getRating());
        }

        return ids.stream().map(id -> {
            Object[] row = stats.get(id);
            long count = row == null ? 0 : ((Number) row[1]).longValue();
            BigDecimal average = row == null ? null : RatingMath.round(((Number) row[2]).doubleValue());
            return new TrackRatingResponse(id, count, average, mine.get(id));
        }).toList();
    }
}
