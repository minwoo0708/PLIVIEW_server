package com.musicreview.server.feed;

import com.musicreview.server.feed.dto.PopularItemResponse;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.PopularItemRow;
import com.musicreview.server.review.domain.RatingMath;
import com.musicreview.server.review.domain.Review;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 집계 행 → 화면용 응답. 표시 정보(이름·이미지)는 그 항목의 가장 최근 리뷰에서 가져옴.
 * 항목마다 조회하지 않고 타입별로 한 번씩(최대 3번) 묶어서 조회합니다.
 */
@Component
@RequiredArgsConstructor
public class PopularItemMapper {

    private final ReviewRepository reviewRepository;

    public List<PopularItemResponse> toResponses(List<PopularItemRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<ItemType, Set<String>> idsByType = new EnumMap<>(ItemType.class);
        for (PopularItemRow row : rows) {
            idsByType.computeIfAbsent(row.type(), type -> new java.util.HashSet<>()).add(row.spotifyId());
        }
        Map<String, ReviewResponse.Item> items = new HashMap<>();
        idsByType.forEach((type, ids) -> {
            for (Review review : reviewRepository.findLatestPerItem(type, ids)) {
                items.put(key(type, review.getSpotifyId()), new ReviewResponse.Item(
                        review.getItemName(), review.getItemSubtitle(), review.getItemImageUrl()));
            }
        });
        return rows.stream()
                .map(row -> new PopularItemResponse(row.type(), row.spotifyId(),
                        items.get(key(row.type(), row.spotifyId())),
                        row.reviewCount(), RatingMath.round(row.averageRating())))
                .collect(Collectors.toList());
    }

    private static String key(ItemType type, String spotifyId) {
        return type.name() + ":" + spotifyId;
    }
}
