package hu.anitrack.security;

import hu.anitrack.entity.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Runs on every request: reads the JWT from the HttpOnly cookie, verifies the signature
 * and the expiry, and if everything is fine, logs the user in for this request.
 * <p>
 * A missing, expired or tampered token is NOT an error here: the request simply stays
 * anonymous, and the authorization rules decide (public endpoint: fine, protected: 401).
 */
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final String cookieName;

    public JwtCookieAuthenticationFilter(JwtDecoder jwtDecoder, String cookieName) {
        this.jwtDecoder = jwtDecoder;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = readToken(request);
        if (token != null) {
            try {
                Jwt jwt = jwtDecoder.decode(token);   // checks signature and expiry
                AuthenticatedUser principal = new AuthenticatedUser(
                        Long.valueOf(jwt.getSubject()),
                        jwt.getClaimAsString("username"),
                        Role.valueOf(jwt.getClaimAsString("role")));

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (RuntimeException ex) {
                // invalid / expired / tampered token: stay anonymous
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}