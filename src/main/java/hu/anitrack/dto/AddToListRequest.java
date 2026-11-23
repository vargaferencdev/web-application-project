package hu.anitrack.dto;

import hu.anitrack.entity.enums.ListStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AddToListRequest(
        @NotNull Long mediaId,
        @NotNull ListStatus status,
        @PositiveOrZero Integer progress   // optional, defaults to 0
) {
}