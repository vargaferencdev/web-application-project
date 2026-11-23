package hu.anitrack.mapper;

import hu.anitrack.dto.UserResponse;
import hu.anitrack.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}