package hu.anitrack.dto;

public record RatingSummaryResponse(
        long total,
        long positive,
        long neutral,
        long negative,
        double positivePercent,
        double neutralPercent,
        double negativePercent
) {
}