package hu.anitrack.dto;

import hu.anitrack.entity.Genre;
import hu.anitrack.entity.Media;
import hu.anitrack.entity.Tag;
import hu.anitrack.entity.enums.MediaFormat;
import hu.anitrack.entity.enums.MediaStatus;
import hu.anitrack.entity.enums.MediaType;

import java.time.LocalDate;
import java.util.List;

/** The detail page of one title, with genres, tags and the rating summary. */
public record MediaDetailResponse(
        Long id,
        String title,
        String description,
        MediaType mediaType,
        MediaFormat format,
        MediaStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Integer totalEpisodes,
        Integer totalChapters,
        Integer totalVolumes,
        Integer durationMinutes,
        String coverImageUrl,
        List<String> genres,
        List<String> tags,
        RatingSummaryResponse ratings
) {
    // must be called inside a transaction: genres and tags are loaded lazily
    public static MediaDetailResponse from(Media media, RatingSummaryResponse ratings) {
        return new MediaDetailResponse(
                media.getId(),
                media.getTitle(),
                media.getDescription(),
                media.getMediaType(),
                media.getFormat(),
                media.getStatus(),
                media.getStartDate(),
                media.getEndDate(),
                media.getTotalEpisodes(),
                media.getTotalChapters(),
                media.getTotalVolumes(),
                media.getDurationMinutes(),
                media.getCoverImageUrl(),
                media.getGenres().stream().map(Genre::getName).sorted().toList(),
                media.getTags().stream().map(Tag::getName).sorted().toList(),
                ratings);
    }
}