package hu.anitrack.controller;

import hu.anitrack.dto.GenreShareResponse;
import hu.anitrack.dto.UserStatsResponse;
import hu.anitrack.security.AuthenticatedUser;
import hu.anitrack.service.StatisticsService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/genres")
    public List<GenreShareResponse> getTopGenres(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "6") int limit) {
        return statisticsService.getTopGenres(user.id(), limit);
    }

    @GetMapping
    public UserStatsResponse getStatistics(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return statisticsService.getStats(user.id());
    }
}
