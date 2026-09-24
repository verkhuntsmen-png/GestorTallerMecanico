package com.refaccionaria.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(min = 10, max = 128)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                    message = "La contrasena debe incluir mayusculas, minusculas, numeros y simbolos")
            String password
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 160) String email,
            @NotBlank @Size(max = 128) String password
    ) {
    }

    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 160) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 10, max = 128)
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
                    message = "La contrasena debe incluir mayusculas, minusculas, numeros y simbolos")
            String newPassword
    ) {
    }

    public record RoleRequest(
            @NotBlank @Size(max = 60) String name,
            @Size(max = 180) String description,
            @NotEmpty Set<@NotBlank @Size(max = 80) String> permissions
    ) {
    }

    public record RoleResponse(Long id, String name, String description, Set<String> permissions) {
    }

    public record UserResponse(Long id, String fullName, String email, Set<RoleResponse> roles, Instant createdAt) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record MessageResponse(String message) {
    }

    public record PasswordResetDevResponse(String message, String resetToken) {
    }
}
