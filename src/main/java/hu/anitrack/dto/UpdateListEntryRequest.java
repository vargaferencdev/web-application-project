package hu.anitrack.dto;

import hu.anitrack.entity.enums.ListStatus;
import jakarta.validation.constraints.PositiveOrZero;

// both fields are optional: only what is sent gets changed
public record UpdateListEntryRequest(
        ListStatus status,
        @PositiveOrZero Integer progress
) {
}