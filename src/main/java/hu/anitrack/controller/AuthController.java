package hu.anitrack.controller;

import hu.anitrack.dto.LoginRequest;
import hu.anitrack.dto.RegisterRequest;
import hu.anitrack.dto.UserResponse;
import hu.anitrack.security.AuthenticatedUser;
import hu.anitrack.security.JwtService;
import hu.anitrack.service.AuthService;
import hu.anitrack.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    /** The token is returned in an HttpOnly cookie, never in the response body. */
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.createCookie(result.token()).toString())
                .body(result.user());
    }

    /** Stateless JWT: logout means deleting the cookie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, jwtService.clearCookie().toString())
                .build();
    }

    /** Who am I? Needs a valid token, otherwise 401. */
    @GetMapping("/me")
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
        return userService.getById(user.id());
    }
}