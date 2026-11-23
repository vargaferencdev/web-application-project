package hu.anitrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT settings from application.yml (prefix app.jwt).
 * The secret is NOT in the repository: it comes from application-local.yml or the JWT_SECRET variable.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("60") long expirationMinutes,
        @DefaultValue("anitrack_token") String cookieName,
        @DefaultValue("false") boolean cookieSecure
) {
}
