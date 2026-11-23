package hu.anitrack.dto;

public record TopRatedResponse(
        Long mediaId,
        String title,
        String mediaType,
        Long votes,
        Double positivePercent
) {
}