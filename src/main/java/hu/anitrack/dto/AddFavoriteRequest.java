package hu.anitrack.dto;

import jakarta.validation.constraints.NotNull;

public record AddFavoriteRequest(
        @NotNull Long mediaId
) {
}