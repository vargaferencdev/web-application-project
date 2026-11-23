package hu.anitrack.dto;

/** Used by the statistics page: how many titles of a user's list belong to a genre. */
public record GenreShareResponse(
        String name,
        long titleCount
) {
}
