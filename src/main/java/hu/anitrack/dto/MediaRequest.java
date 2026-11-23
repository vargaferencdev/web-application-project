package hu.anitrack.dto;

import hu.anitrack.entity.enums.MediaFormat;
import hu.anitrack.entity.enums.MediaStatus;
import hu.anitrack.entity.enums.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/**
 * Admin request to create or replace a title (PUT replaces every field).
 * Cross-field rules (format/status must match the type, anime vs. manga fields)
 * are checked in AdminCatalogService, and again by the database CHECK constraints.
 */
public record MediaRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 10000) String description,
        @NotNull MediaType mediaType,
        @NotNull MediaFormat format,
        @NotNull MediaStatus status,
        LocalDate startDate,
        LocalDate endDate,
        @Positive Integer totalEpisodes,
        @Positive Integer totalChapters,
        @Positive Integer totalVolumes,
        @Positive Integer durationMinutes,
        @Size(max = 500) String coverImageUrl,
        Set<Long> genreIds,
        Set<Long> tagIds
) {
}