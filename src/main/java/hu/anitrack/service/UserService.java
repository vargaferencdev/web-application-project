package hu.anitrack.service;

import hu.anitrack.dto.RegisterRequest;
import hu.anitrack.dto.UserResponse;
import hu.anitrack.entity.User;
import hu.anitrack.entity.UserProfile;
import hu.anitrack.entity.enums.Role;
import hu.anitrack.exception.DuplicateResourceException;
import hu.anitrack.exception.ResourceNotFoundException;
import hu.anitrack.mapper.UserMapper;
import hu.anitrack.repository.UserProfileRepository;
import hu.anitrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Registers a new USER and creates the empty profile (1:1) in the same transaction:
     * either both rows are saved or neither.
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("E-mail is already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);   // admins are never created through registration
        User saved = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(saved);
        userProfileRepository.save(profile);

        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}