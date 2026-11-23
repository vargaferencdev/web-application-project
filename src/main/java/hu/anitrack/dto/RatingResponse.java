package hu.anitrack.dto;

import hu.anitrack.entity.enums.Reaction;

public record RatingResponse(
        Long mediaId,
        Reaction reaction
) {
}
