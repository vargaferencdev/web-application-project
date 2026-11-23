package hu.anitrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Used for creating and renaming genres and tags. */
public record NameRequest(
        @NotBlank @Size(max = 50) String name
) {
}