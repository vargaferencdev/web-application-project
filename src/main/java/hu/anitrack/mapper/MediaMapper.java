package hu.anitrack.mapper;

import hu.anitrack.dto.MediaSummaryResponse;
import hu.anitrack.entity.Media;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MediaMapper {

    MediaSummaryResponse toSummary(Media media);
}