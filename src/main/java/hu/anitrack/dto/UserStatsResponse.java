package hu.anitrack.dto;

public record UserStatsResponse(
        long totalTitles,
        long animeCount,
        long mangaCount,
        long episodesWatched,
        long chaptersRead,
        long minutesWatched,
        double estimatedDaysWatched   // an estimate: episodes watched x episode length
) {
}