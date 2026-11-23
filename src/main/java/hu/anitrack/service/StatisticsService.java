package hu.anitrack.service;

import hu.anitrack.dto.UserStatsResponse;
import hu.anitrack.repository.UserMediaListRepository;
import hu.anitrack.repository.projection.UserStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private static final double MINUTES_PER_DAY = 1440.0;

    private final UserMediaListRepository listRepository;

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