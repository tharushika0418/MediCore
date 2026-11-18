package com.medicore.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /login.
 * <p>
 * The password arrives in plain text (over HTTPS in production) and is only
 * compared against the stored BCrypt hash; it is never stored or logged.
 */
public class LoginRequestDTO {

    @Schema(description = "Registered email address (the login name)",
            example = "testuser@test.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be a valid email address")
    private String email;

    @Schema(description = "Plain-text password, compared with the stored BCrypt hash",
            example = "password123", minLength = 8, format = "password")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
