package hu.anitrack.service;

import hu.anitrack.dto.GenreShareResponse;
import hu.anitrack.dto.UserStatsResponse;
import hu.anitrack.repository.UserMediaListRepository;
import hu.anitrack.repository.projection.UserStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final double MINUTES_PER_DAY = 1440.0;

    private final UserMediaListRepository listRepository;

    /** The user's favourite genres, by the number of titles on their list. */
    @Transactional(readOnly = true)
    public List<GenreShareResponse> getTopGenres(Long userId, int limit) {
        return listRepository.findTopGenres(userId, Math.max(1, Math.min(limit, 20))).stream()
                .map(g -> new GenreShareResponse(g.getName(), g.getTitleCount()))
                .toList();
    }

    /** Nothing is stored: the numbers are calculated from the user's list on every request. */
    @Transactional(readOnly = true)
    public UserStatsResponse getStats(Long userId) {
        UserStats stats = listRepository.getUserStats(userId);
        long minutes = stats.getMinutesWatched();
        double days = Math.round(minutes / MINUTES_PER_DAY * 10) / 10.0;   // one decimal
        return new UserStatsResponse(
                stats.getTotalTitles(),
                stats.getAnimeCount(),
                stats.getMangaCount(),
                stats.getEpisodesWatched(),
                stats.getChaptersRead(),
                minutes,
                days);
    }
}
