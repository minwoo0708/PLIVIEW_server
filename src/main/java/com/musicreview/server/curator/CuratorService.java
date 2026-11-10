package com.musicreview.server.curator;

import com.musicreview.server.curator.dto.CuratorRankingResponse;
import com.musicreview.server.review.domain.CuratorRow;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.RatingMath;
import com.musicreview.server.review.domain.Review;
import com.musicreview.server.review.domain.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CuratorService {

    private final ReviewRepository reviewRepository;

    /**
     * 플리 주인별 평균 총점 순위.
     * 화면에 보이는 값(소수 첫째 자리 평균)이 같으면 같은 순위: 1, 1, 3 방식.
     */
    public List<CuratorRankingResponse> getRanking(int minReviews, int limit) {
        List<CuratorRow> rows = reviewRepository.findCuratorRanking(minReviews, PageRequest.of(0, limit));

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
            result.add(new CuratorRankingResponse(
                    rank,
                    row.ownerSpotifyId(),
                    ownerName(row.ownerSpotifyId()),
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

    /** 주인의 가장 최근 플리 리뷰에 저장된 부제목(주인 표시 이름). 없으면 Spotify id */
    private String ownerName(String ownerSpotifyId) {
        return reviewRepository
                .findFirstByTypeAndOwnerSpotifyIdOrderByCreatedAtDescIdDesc(ItemType.PLAYLIST, ownerSpotifyId)
                .map(Review::getItemSubtitle)
                .filter(name -> !name.isBlank())
                .orElse(ownerSpotifyId);
    }
}
