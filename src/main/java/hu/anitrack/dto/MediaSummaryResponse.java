package hu.anitrack.dto;

import hu.anitrack.entity.enums.MediaFormat;
import hu.anitrack.entity.enums.MediaStatus;
import hu.anitrack.entity.enums.MediaType;

import java.time.LocalDate;

/** One card in the catalog list. */
public record MediaSummaryResponse(
        Long id,
        String title,
        MediaType mediaType,
        MediaFormat format,
        MediaStatus status,
        LocalDate startDate,
        Integer totalEpisodes,
        Integer totalChapters,
        String coverImageUrl
) {
}