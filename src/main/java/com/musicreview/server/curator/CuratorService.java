package com.musicreview.server.curator;

import com.musicreview.server.curator.dto.CuratorProfileResponse;
import com.musicreview.server.curator.dto.CuratorRankingResponse;
import com.musicreview.server.feed.PopularItemMapper;
import com.musicreview.server.feed.dto.PopularItemResponse;
import com.musicreview.server.global.common.PageResponse;
import com.musicreview.server.global.common.TimeUtils;
import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.review.domain.CuratorRow;
import com.musicreview.server.review.domain.RatingMath;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.user.domain.User;
import com.musicreview.server.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 베스트 큐레이터 = Spotify를 연동한 PLIVIEW 사용자. 그 사람이 만든(owner) 플리에 달린 리뷰로 평가 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuratorService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final PopularItemMapper popularItemMapper;

    /**
     * 평균 총점 순위.
     * 화면에 보이는 값(소수 첫째 자리 평균)이 같으면 같은 순위: 1, 1, 3 방식.
     */
    public List<CuratorRankingResponse> getRanking(int minReviews, int limit) {
        List<CuratorRow> rows = reviewRepository.findCuratorRanking(minReviews, PageRequest.of(0, limit));
        Map<Long, User> users = userRepository.findAllById(rows.stream().map(CuratorRow::userId).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        List<CuratorRankingResponse> result = new ArrayList<>(rows.size());
        BigDecimal previousAverage = null;
        int rank = 0;
        for (int i = 0; i < rows.size(); i++) {
            CuratorRow row = rows.get(i);
            BigDecimal average = RatingMath.round(row.averageRating());
            if (previousAverage == null || average.compareTo(previousAverage) != 0) {
                rank = i + 1;
                previousAverage = average;
            }
            User user = users.get(row.userId());
            result.add(new CuratorRankingResponse(
                    rank,
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getSpotifyUserId(),
                    user.getSpotifyDisplayName(),
                    row.playlistCount(),
                    row.reviewCount(),
                    average,
                    new CuratorRankingResponse.AverageScores(
                            RatingMath.round(row.averageConcept()),
                            RatingMath.round(row.averageFlow()),
                            RatingMath.round(row.averageDiscovery()))));
        }
        return result;
    }

    /** 큐레이터 프로필 상단. Spotify를 연동하지 않은 사용자면 404 CURATOR_NOT_FOUND */
    public CuratorProfileResponse getProfile(Long userId) {
        User user = getCurator(userId);
        Object[] totals = reviewRepository.findCuratorTotals(user.getSpotifyUserId()).getFirst();
        return new CuratorProfileResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getSpotifyUserId(),
                user.getSpotifyDisplayName(),
                toLong(totals[0]),
                toLong(totals[1]),
                RatingMath.round(toDouble(totals[2])),
                new CuratorRankingResponse.AverageScores(
                        RatingMath.round(toDouble(totals[3])),
                        RatingMath.round(toDouble(totals[4])),
                        RatingMath.round(toDouble(totals[5]))),
                TimeUtils.toOffset(user.getCreatedAt()));
    }

    /** 큐레이터의 플리를 인기순(리뷰 수 → 평균 총점)으로. PLIVIEW에서 리뷰가 달린 플리만 나옴 */
    public PageResponse<PopularItemResponse> getPlaylists(Long userId, int page, int size) {
        User user = getCurator(userId);
        return PageResponse.of(
                reviewRepository.findCuratorPlaylists(user.getSpotifyUserId(), PageRequest.of(page, size)),
                popularItemMapper::toResponse);
    }

    private User getCurator(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isSpotifyLinked)
                .orElseThrow(() -> new BusinessException(ErrorCode.CURATOR_NOT_FOUND));
    }

    private static long toLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }
}
