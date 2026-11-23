package hu.anitrack.config;

import hu.anitrack.security.JwtCookieAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder jwtDecoder,
                                                   JwtProperties jwtProperties) throws Exception {
        http
                // CSRF protection is off: there is no server session, and the JWT cookie is
                // SameSite=Lax, so browsers do not send it on cross-site POST/PUT/DELETE requests.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // not logged in -> 401 (the default would be 403)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // registration, login, logout
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/logout").permitAll()
                        // the public catalog (read-only). Never put per-user data under these GET paths!
                        .requestMatchers(HttpMethod.GET, "/api/media/**", "/api/genres/**", "/api/tags/**").permitAll()
                        // API documentation
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // admin only
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // every other API call needs a logged-in user
                        .requestMatchers("/api/**").authenticated()
                        // static frontend files (HTML, CSS, JS)
                        .anyRequest().permitAll())
                .addFilterBefore(
                        new JwtCookieAuthenticationFilter(jwtDecoder, jwtProperties.cookieName()),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}