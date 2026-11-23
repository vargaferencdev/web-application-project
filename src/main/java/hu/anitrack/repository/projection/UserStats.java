package hu.anitrack.repository.projection;

public interface UserStats {

    Long getTotalTitles();

    Long getAnimeCount();

    Long getMangaCount();

    Long getEpisodesWatched();

    Long getChaptersRead();

    Long getMinutesWatched();
}
