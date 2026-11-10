package com.musicreview.server.item;

import com.musicreview.server.global.security.CurrentUserId;
import com.musicreview.server.item.dto.TrackRatingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TrackRatingController {

    private final TrackRatingService trackRatingService;

    /** 앨범·플리 화면의 수록곡 목록에 곡별 평점을 한 번에 붙이기 */
    @GetMapping("/api/items/tracks/ratings")
    public List<TrackRatingResponse> ratings(@CurrentUserId Long userId, @RequestParam List<String> spotifyIds) {
        return trackRatingService.getRatings(userId, spotifyIds);
    }
}
