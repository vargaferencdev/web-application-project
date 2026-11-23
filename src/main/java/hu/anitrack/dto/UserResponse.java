package hu.anitrack.dto;

import hu.anitrack.entity.enums.Role;

import java.time.Instant;

// never contains the password hash
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        Instant createdAt
) {
}