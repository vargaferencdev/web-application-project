package hu.anitrack.dto;

public record PopularMediaResponse(
        Long mediaId,
        String title,
        String mediaType,
        Long listCount,
        String coverImageUrl
) {
}
