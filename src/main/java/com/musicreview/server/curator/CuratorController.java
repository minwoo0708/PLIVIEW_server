package com.musicreview.server.curator;

import com.musicreview.server.curator.dto.CuratorRankingResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/curators")
@RequiredArgsConstructor
public class CuratorController {

    private final CuratorService curatorService;

    @GetMapping("/ranking")
    public List<CuratorRankingResponse> ranking(
            @RequestParam(defaultValue = "3") @Min(value = 1, message = "minReviews는 1~100입니다.")
            @Max(value = 100, message = "minReviews는 1~100입니다.") int minReviews,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "limit은 1~100입니다.")
            @Max(value = 100, message = "limit은 1~100입니다.") int limit) {
        return curatorService.getRanking(minReviews, limit);
    }
}
