package com.medicore.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for a successful POST /login.
 * <p>
 * The client sends this token on later requests as:
 * {@code Authorization: Bearer <token>}
 */
public class LoginResponseDTO {

    @Schema(description = "Signed JWT (HS256), valid for 10 hours. "
            + "Send it as: Authorization: Bearer <token>",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlckB0ZXN0LmNvbSIsInJvbGUiOiJBRE1JTiJ9.signature")
    private final String token;

    public LoginResponseDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
