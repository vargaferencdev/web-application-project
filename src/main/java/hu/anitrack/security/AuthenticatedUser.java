package hu.anitrack.security;

import hu.anitrack.entity.enums.Role;

/**
 * The logged-in user as the controllers see it (built from the token claims).
 * Controllers receive it with @AuthenticationPrincipal.
 */
public record AuthenticatedUser(Long id, String username, Role role) {
}
