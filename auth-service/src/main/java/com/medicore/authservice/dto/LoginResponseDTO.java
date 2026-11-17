package com.medicore.authservice.dto;

/**
 * Response body for a successful POST /login.
 * <p>
 * The client sends this token on later requests as:
 * {@code Authorization: Bearer <token>}
 */
public class LoginResponseDTO {

    private final String token;

    public LoginResponseDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
