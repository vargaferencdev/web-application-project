package hu.anitrack.mapper;

import hu.anitrack.dto.FavoriteResponse;
import hu.anitrack.entity.Favorite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    @Mapping(target = "mediaId", source = "media.id")
    @Mapping(target = "title", source = "media.title")
    @Mapping(target = "mediaType", source = "media.mediaType")
    @Mapping(target = "coverImageUrl", source = "media.coverImageUrl")
    FavoriteResponse toResponse(Favorite favorite);
}