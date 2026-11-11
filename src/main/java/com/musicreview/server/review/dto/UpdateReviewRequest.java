package com.musicreview.server.review.dto;

import com.musicreview.server.global.validation.Score;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * PATCH: null이거나 빠진 필드는 그대로 둠.
 * comment ""이면 삭제, tags []이면 모두 삭제. type, spotifyId, ownerSpotifyId는 변경 불가(보내도 무시).
 */
public record UpdateReviewRequest(
        @Valid
        ItemInfoRequest item,

        @Score
        BigDecimal rating,

        @Size(max = 200, message = "한 줄 평은 200자 이하여야 합니다.")
        String comment,

        @Size(max = 5, message = "태그는 5개까지 달 수 있습니다.")
        List<@NotBlank(message = "빈 태그는 달 수 없습니다.") @Size(max = 20, message = "태그는 20자 이하여야 합니다.") String> tags,

        @Score
        BigDecimal conceptScore,

        @Score
        BigDecimal flowScore,

        @Score
        BigDecimal discoveryScore,

        @Size(max = 100, message = "snapshotId는 100자 이하여야 합니다.")
        String snapshotId,

        // ===== 앨범·플리 하이라이트: null이면 그대로, bestMoment·body는 ""이면 삭제 =====

        @Valid
        TrackRefRequest bestTrack,

        @Valid
        TrackRefRequest worstTrack,

        @Valid
        TrackRefRequest underratedTrack,

        @Size(max = 100, message = "최고의 순간은 100자 이하여야 합니다.")
        String bestMoment,

        @Size(max = 2000, message = "전체 평가는 2000자 이하여야 합니다.")
        String body,

        /**
         * 지울 선택 항목 이름. 예: ["bestTrack", "body"]
         * 가능한 값: comment, bestTrack, worstTrack, underratedTrack, bestMoment, body
         * 같은 항목을 값으로도 보내고 clear에도 넣으면 400
         */
        @Size(max = 6, message = "clear에는 최대 6개까지 넣을 수 있습니다.")
        Set<String> clear
) {

    /** clear에 넣을 수 있는 항목 */
    public static final Set<String> CLEARABLE =
            Set.of("comment", "bestTrack", "worstTrack", "underratedTrack", "bestMoment", "body");

    public Set<String> clearOrEmpty() {
        return clear == null ? Set.of() : clear;
    }

    public boolean hasPlaylistFields() {
        return conceptScore != null || flowScore != null || discoveryScore != null || snapshotId != null;
    }

    public boolean hasHighlights() {
        return bestTrack != null || worstTrack != null || underratedTrack != null
                || bestMoment != null || body != null
                || clearOrEmpty().stream().anyMatch(name -> !name.equals("comment"));
    }

    /** clear에 들어 있는데 값도 함께 보낸 항목 */
    public boolean isSetAndCleared(String name) {
        if (!clearOrEmpty().contains(name)) {
            return false;
        }
        return switch (name) {
            case "comment" -> comment != null;
            case "bestTrack" -> bestTrack != null;
            case "worstTrack" -> worstTrack != null;
            case "underratedTrack" -> underratedTrack != null;
            case "bestMoment" -> bestMoment != null;
            case "body" -> body != null;
            default -> false;
        };
    }
}
