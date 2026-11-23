package hu.anitrack.mapper;

import hu.anitrack.dto.ListEntryResponse;
import hu.anitrack.entity.Media;
import hu.anitrack.entity.UserMediaList;
import hu.anitrack.entity.enums.MediaType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ListEntryMapper {

    @Mapping(target = "mediaId", source = "media.id")
    @Mapping(target = "title", source = "media.title")
    @Mapping(target = "mediaType", source = "media.mediaType")
    @Mapping(target = "coverImageUrl", source = "media.coverImageUrl")
    @Mapping(target = "total", source = "media", qualifiedByName = "totalOf")
    ListEntryResponse toResponse(UserMediaList entry);

    @Named("totalOf")
    default Integer totalOf(Media media) {
        return media.getMediaType() == MediaType.ANIME
                ? media.getTotalEpisodes()
                : media.getTotalChapters();
    }
}
