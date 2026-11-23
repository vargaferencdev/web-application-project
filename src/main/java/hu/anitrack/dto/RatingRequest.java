package hu.anitrack.dto;

import hu.anitrack.entity.enums.Reaction;
import jakarta.validation.constraints.NotNull;

public record RatingRequest(
        @NotNull Reaction reaction
) {
}