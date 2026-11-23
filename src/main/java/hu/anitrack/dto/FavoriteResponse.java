package hu.anitrack.dto;

import hu.anitrack.entity.enums.MediaType;

public record FavoriteResponse(
        Long mediaId,
        String title,
        MediaType mediaType,
        String coverImageUrl,
        Integer position
) {
}
