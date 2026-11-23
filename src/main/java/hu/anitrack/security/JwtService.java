package hu.anitrack.security;

import hu.anitrack.config.JwtProperties;
import hu.anitrack.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    /**
     * Token claims: sub (user id), username, role, iat, exp.
     * No password, hash or e-mail: the payload is signed but NOT encrypted.
     */
    public String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(properties.expirationMinutes(), ChronoUnit.MINUTES))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** HttpOnly: JavaScript cannot read it (XSS). SameSite=Lax: not sent on cross-site POST/PUT/DELETE (CSRF). */
    public ResponseCookie createCookie(String token) {
        return baseCookie(token)
                .maxAge(Duration.ofMinutes(properties.expirationMinutes()))
                .build();
    }

    /** Logout = an expired, empty cookie with the same name and path. */
    public ResponseCookie clearCookie() {
        return baseCookie("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.cookieName(), value)
                .httpOnly(true)
                .secure(properties.cookieSecure())   // true in production (HTTPS)
                .sameSite("Lax")
                .path("/");
    }
}