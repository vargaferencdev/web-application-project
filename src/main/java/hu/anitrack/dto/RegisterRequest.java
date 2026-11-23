package hu.anitrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank
        @Size(min = 3, max = 30)
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Only letters, digits and underscore are allowed")
        String username,

        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        // BCrypt only uses the first 72 bytes of a password
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}