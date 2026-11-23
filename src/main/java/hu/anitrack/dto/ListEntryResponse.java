package hu.anitrack.dto;

import hu.anitrack.entity.enums.ListStatus;
import hu.anitrack.entity.enums.MediaType;

import java.time.Instant;
import java.time.LocalDate;

public record ListEntryResponse(
        Long id,
        Long mediaId,
        String title,
        MediaType mediaType,
        ListStatus status,
        Integer progress,
        Integer total,          // total episodes (anime) or chapters (manga), null if unknown
        LocalDate startedAt,
        LocalDate completedAt,
        Instant updatedAt,
        String coverImageUrl
) {
}
