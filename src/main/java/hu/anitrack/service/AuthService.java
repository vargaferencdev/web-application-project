package hu.anitrack.service;

import hu.anitrack.dto.LoginRequest;
import hu.anitrack.dto.UserResponse;
import hu.anitrack.entity.User;
import hu.anitrack.mapper.UserMapper;
import hu.anitrack.repository.UserRepository;
import hu.anitrack.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public record AuthResult(String token, UserResponse user) {
    }

    /**
     * Checks the credentials and issues a token. Unknown user and wrong password give the
     * same message, so the response does not reveal which usernames exist.
     */
    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        return new AuthResult(jwtService.createToken(user), userMapper.toResponse(user));
    }
}
