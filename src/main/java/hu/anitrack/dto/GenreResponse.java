package hu.anitrack.dto;

public record GenreResponse(
        Long id,
        String name,
        Long mediaCount
) {
}