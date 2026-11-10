package com.musicreview.server.curator;

import com.musicreview.server.curator.dto.CuratorProfileResponse;
import com.musicreview.server.curator.dto.CuratorRankingResponse;
import com.musicreview.server.feed.dto.PopularItemResponse;
import com.musicreview.server.global.common.PageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/curators")
@RequiredArgsConstructor
public class CuratorController {

    private final CuratorService curatorService;

    /** 베스트 큐레이터: Spotify를 연동한 사용자만 */
    @GetMapping("/ranking")
    public List<CuratorRankingResponse> ranking(
            @RequestParam(defaultValue = "3") @Min(value = 1, message = "minReviews는 1~100입니다.")
            @Max(value = 100, message = "minReviews는 1~100입니다.") int minReviews,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit은 1~100입니다.")
            @Max(value = 100, message = "limit은 1~100입니다.") int limit) {
        return curatorService.getRanking(minReviews, limit);
    }

    /** 큐레이터 프로필 (랭킹에서 누르면 나오는 화면 상단) */
    @GetMapping("/{userId}")
    public CuratorProfileResponse profile(@PathVariable Long userId) {
        return curatorService.getProfile(userId);
    }

    /** 큐레이터의 플리 목록, 인기순 */
    @GetMapping("/{userId}/playlists")
    public PageResponse<PopularItemResponse> playlists(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size는 1~50입니다.")
            @Max(value = 50, message = "size는 1~50입니다.") int size) {
        return curatorService.getPlaylists(userId, page, size);
    }
}
