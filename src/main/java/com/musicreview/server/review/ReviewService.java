package com.musicreview.server.review;

import com.musicreview.server.global.exception.BusinessException;
import com.musicreview.server.global.exception.ErrorCode;
import com.musicreview.server.global.validation.SpotifyId;
import com.musicreview.server.review.domain.ItemType;
import com.musicreview.server.review.domain.Review;
import com.musicreview.server.review.domain.ReviewRepository;
import com.musicreview.server.review.dto.CreateReviewRequest;
import com.musicreview.server.review.dto.ItemInfoRequest;
import com.musicreview.server.spotify.SpotifyItemLookup;
import com.musicreview.server.review.dto.ReviewResponse;
import com.musicreview.server.review.dto.TrackRefRequest;
import com.musicreview.server.review.dto.UpdateReviewRequest;
import com.musicreview.server.user.UserService;
import com.musicreview.server.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewService {

    static final int MINE_MAX_IDS = 50;

    private final ReviewRepository reviewRepository;
    private final UserService userService;
    private final SpotifyItemLookup itemLookup;
    private final TransactionTemplate transactionTemplate;

    /**
     * 트랜잭션 밖에서 Spotify를 먼저 조회하고, DB에 쓰는 부분만 트랜잭션으로 묶는다.
     * (외부 HTTP 호출이 느려도 DB 연결을 붙잡고 있지 않게)
     */
    public ReviewResponse create(Long userId, CreateReviewRequest request) {
        validateTypeSpecificFields(request);
        userService.getUser(userId);
        if (reviewRepository.existsByUserIdAndTypeAndSpotifyId(userId, request.type(), request.spotifyId())) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // Spotify에서 진짜 정보를 확인할 수 있으면 그 값을 쓰고, 안 되면 앱이 보낸 값을 씀
        Optional<SpotifyItemLookup.SpotifyItem> verified = itemLookup.lookup(request.type(), request.spotifyId());
        return transactionTemplate.execute(status -> save(userId, request, verified));
    }

    private ReviewResponse save(Long userId, CreateReviewRequest request,
                                Optional<SpotifyItemLookup.SpotifyItem> verified) {
        User user = userService.getUser(userId);
        if (reviewRepository.existsByUserIdAndTypeAndSpotifyId(userId, request.type(), request.spotifyId())) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Review review = Review.create(user, request.type(), request.spotifyId());
        applyItem(review, request.item(), verified);
        review.changeRating(scale(request.rating()));
        review.changeComment(request.comment());
        review.replaceTags(normalizeTags(request.tags()));
        if (review.isPlaylist()) {
            review.changePlaylistScores(scale(request.conceptScore()), scale(request.flowScore()), scale(request.discoveryScore()));
            review.changeSnapshotId(request.snapshotId() != null
                    ? request.snapshotId()
                    : verified.map(SpotifyItemLookup.SpotifyItem::snapshotId).orElse(null));
            // 큐레이터 랭킹 조작을 막기 위해 플리 주인 id는 Spotify 값을 우선
            review.assignOwner(verified.map(SpotifyItemLookup.SpotifyItem::ownerId)
                    .orElse(request.ownerSpotifyId().strip()));
        }
        applyHighlights(review, request.bestTrack(), request.worstTrack(), request.underratedTrack(),
                request.bestMoment(), request.body());

        try {
            reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException e) {
            // 같은 항목에 동시에 두 번 작성한 경우 (UNIQUE 제약)
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
        return ReviewResponse.of(review, userId);
    }

    @Transactional(readOnly = true)
    public ReviewResponse get(Long userId, Long reviewId) {
        return ReviewResponse.of(getReview(reviewId), userId);
    }

    public ReviewResponse update(Long userId, Long reviewId, UpdateReviewRequest request) {
        // 검증과 Spotify 조회는 트랜잭션 밖에서, 저장만 트랜잭션 안에서
        Review current = getOwnReview(userId, reviewId);
        validateUpdate(current, request);
        Optional<SpotifyItemLookup.SpotifyItem> verified = request.item() == null
                ? Optional.empty()
                : itemLookup.lookup(current.getType(), current.getSpotifyId());
        return transactionTemplate.execute(status -> applyUpdate(userId, reviewId, request, verified));
    }

    private void validateUpdate(Review review, UpdateReviewRequest request) {
        if (!review.isPlaylist() && request.hasPlaylistFields()) {
            throw new BusinessException(ErrorCode.INVALID_REVIEW_FIELDS,
                    "곡·앨범 리뷰에는 conceptScore, flowScore, discoveryScore, snapshotId를 보낼 수 없습니다.");
        }
        validateHighlights(review.getType(), request.hasHighlights(),
                request.underratedTrack() != null || request.clearOrEmpty().contains("underratedTrack"));
        validateClear(request);
    }

    private ReviewResponse applyUpdate(Long userId, Long reviewId, UpdateReviewRequest request,
                                       Optional<SpotifyItemLookup.SpotifyItem> verified) {
        Review review = getOwnReview(userId, reviewId);
        if (request.item() != null) {
            applyItem(review, request.item(), verified);
        }
        if (request.rating() != null) {
            review.changeRating(scale(request.rating()));
        }
        if (request.comment() != null) {
            review.changeComment(request.comment());
        }
        if (request.tags() != null) {
            review.replaceTags(normalizeTags(request.tags()));
        }
        if (review.isPlaylist()) {
            review.changePlaylistScores(scale(request.conceptScore()), scale(request.flowScore()), scale(request.discoveryScore()));
            if (request.snapshotId() != null) {
                review.changeSnapshotId(request.snapshotId());
            }
        }
        applyHighlights(review, request.bestTrack(), request.worstTrack(), request.underratedTrack(),
                request.bestMoment(), request.body());
        applyClear(review, request.clearOrEmpty());
        reviewRepository.flush(); // updatedAt 반영
        return ReviewResponse.of(review, userId);
    }

    @Transactional
    public void delete(Long userId, Long reviewId) {
        reviewRepository.delete(getOwnReview(userId, reviewId));
    }

    /** 여러 항목 중 내가 리뷰한 것만 (최근 재생·검색 결과의 "이미 평가함" 표시용) */
    @Transactional(readOnly = true)
    public List<ReviewResponse> findMine(Long userId, String type, List<String> spotifyIds) {
        ItemType itemType = ItemType.from(type);
        Set<String> ids = new LinkedHashSet<>();
        for (String id : spotifyIds) {
            String trimmed = id.strip();
            if (!trimmed.matches(SpotifyId.REGEX)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "spotifyIds에 올바르지 않은 id가 있습니다: " + trimmed);
            }
            ids.add(trimmed);
        }
        if (ids.isEmpty() || ids.size() > MINE_MAX_IDS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "spotifyIds는 1~50개까지 보낼 수 있습니다.");
        }
        return reviewRepository.findByUserIdAndTypeAndSpotifyIdIn(userId, itemType, ids).stream()
                .map(review -> ReviewResponse.of(review, userId))
                .toList();
    }

    private Review getReview(Long reviewId) {
        return reviewRepository.findWithUserById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
    }

    private Review getOwnReview(Long userId, Long reviewId) {
        Review review = getReview(reviewId);
        if (!review.isWrittenBy(userId)) {
            throw new BusinessException(ErrorCode.REVIEW_FORBIDDEN);
        }
        return review;
    }

    /** 표시용 정보: Spotify에서 확인한 값 우선, 비어 있는 칸만 앱이 보낸 값으로 */
    private static void applyItem(Review review, ItemInfoRequest requested,
                                  Optional<SpotifyItemLookup.SpotifyItem> verified) {
        String name = verified.map(SpotifyItemLookup.SpotifyItem::name).orElse(requested.name().strip());
        String subtitle = verified.map(SpotifyItemLookup.SpotifyItem::subtitle).orElse(requested.subtitle());
        String imageUrl = verified.map(SpotifyItemLookup.SpotifyItem::imageUrl).orElse(requested.imageUrl());
        review.changeItem(truncate(name, 200), truncate(subtitle, 200), truncate(imageUrl, 500));
    }

    /** clear: 허용된 이름만, 값과 동시에 보내면 400 */
    private static void validateClear(UpdateReviewRequest request) {
        for (String name : request.clearOrEmpty()) {
            if (!UpdateReviewRequest.CLEARABLE.contains(name)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "clear에 넣을 수 없는 항목입니다: " + name + " (가능: " + String.join(", ",
                                UpdateReviewRequest.CLEARABLE.stream().sorted().toList()) + ")");
            }
            if (request.isSetAndCleared(name)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        name + "을(를) 값으로 보내면서 clear에도 넣을 수 없습니다.");
            }
        }
    }

    private static void applyClear(Review review, Set<String> clear) {
        for (String name : clear) {
            switch (name) {
                case "comment" -> review.changeComment(null);
                case "bestTrack" -> review.changeBestTrack(null, null);
                case "worstTrack" -> review.changeWorstTrack(null, null);
                case "underratedTrack" -> review.changeUnderratedTrack(null, null);
                case "bestMoment" -> review.changeBestMoment(null);
                case "body" -> review.changeBody(null);
                default -> throw new IllegalStateException(name);
            }
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    /** 하이라이트는 앨범·플리만, 과소평가된 곡은 앨범만 */
    private static void validateHighlights(ItemType type, boolean hasHighlights, boolean hasUnderrated) {
        if (type == ItemType.TRACK && hasHighlights) {
            throw new BusinessException(ErrorCode.INVALID_REVIEW_FIELDS,
                    "곡 리뷰에는 bestTrack, worstTrack, underratedTrack, bestMoment, body를 보낼 수 없습니다.");
        }
        if (type == ItemType.PLAYLIST && hasUnderrated) {
            throw new BusinessException(ErrorCode.INVALID_REVIEW_FIELDS,
                    "underratedTrack(가장 과소평가된 곡)은 앨범 리뷰에만 쓸 수 있습니다.");
        }
    }

    /** null인 항목은 그대로 둠 (작성 시에는 null = 없음) */
    private static void applyHighlights(Review review, TrackRefRequest best, TrackRefRequest worst,
                                        TrackRefRequest underrated, String bestMoment, String body) {
        if (best != null) {
            review.changeBestTrack(best.spotifyId(), best.name().strip());
        }
        if (worst != null) {
            review.changeWorstTrack(worst.spotifyId(), worst.name().strip());
        }
        if (underrated != null) {
            review.changeUnderratedTrack(underrated.spotifyId(), underrated.name().strip());
        }
        if (bestMoment != null) {
            review.changeBestMoment(bestMoment);
        }
        if (body != null) {
            review.changeBody(body);
        }
    }

    /** 플리는 항목 점수 3개 + 주인 id가 필수, 곡·앨범은 플리 전용 필드 금지 */
    private void validateTypeSpecificFields(CreateReviewRequest request) {
        validateHighlights(request.type(), request.hasHighlights(), request.underratedTrack() != null);
        if (request.type() == ItemType.PLAYLIST) {
            List<String> missing = new ArrayList<>();
            if (request.conceptScore() == null) missing.add("conceptScore");
            if (request.flowScore() == null) missing.add("flowScore");
            if (request.discoveryScore() == null) missing.add("discoveryScore");
            if (request.ownerSpotifyId() == null || request.ownerSpotifyId().isBlank()) missing.add("ownerSpotifyId");
            if (!missing.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_REVIEW_FIELDS,
                        "플레이리스트 리뷰에 필요한 항목이 없습니다: " + String.join(", ", missing));
            }
            return;
        }
        boolean hasPlaylistFields = request.conceptScore() != null || request.flowScore() != null
                || request.discoveryScore() != null || request.snapshotId() != null || request.ownerSpotifyId() != null;
        if (hasPlaylistFields) {
            throw new BusinessException(ErrorCode.INVALID_REVIEW_FIELDS,
                    "곡·앨범 리뷰에는 플레이리스트 전용 항목을 보낼 수 없습니다.");
        }
    }

    /** 앞뒤 공백 제거, 중복 제거 */
    private static Set<String> normalizeTags(List<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags != null) {
            tags.forEach(tag -> result.add(tag.strip()));
        }
        return result;
    }

    /** 5 → 5.0 처럼 소수 첫째 자리로 통일 */
    private static BigDecimal scale(BigDecimal score) {
        return score == null ? null : score.setScale(1, RoundingMode.UNNECESSARY);
    }
}
