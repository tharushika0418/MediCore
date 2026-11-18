package com.medicore.authservice.controller;

import com.medicore.authservice.config.OpenApiConfig;
import com.medicore.authservice.dto.LoginRequestDTO;
import com.medicore.authservice.dto.LoginResponseDTO;
import com.medicore.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@Tag(name = "Auth", description = "API for login and token validation")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Generate token on user login",
            description = "Checks the email and password against the stored BCrypt hash and, "
                    + "if they match, returns a signed JWT valid for 10 hours. "
                    + "An unknown email and a wrong password both return 401, so the API "
                    + "does not reveal which emails are registered.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, token returned",
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input: email format or password length",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Wrong email or password",
                    content = @Content)
    })
    public ResponseEntity<LoginResponseDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User credentials",
                    content = @Content(examples = @ExampleObject(
                            name = "Test user",
                            summary = "Seeded test user (data.sql)",
                            value = "{\"email\": \"testuser@test.com\", \"password\": \"password123\"}")))
            @Valid @RequestBody LoginRequestDTO loginRequestDTO) {

        Optional<String> tokenOptional = authService.authenticate(loginRequestDTO);

        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new LoginResponseDTO(tokenOptional.get()));
    }

    @GetMapping("/validate")
    @Operation(
            summary = "Validate token",
            description = "Verifies the JWT's signature and expiry. Used by the API Gateway "
                    + "before forwarding requests to protected services. "
                    + "In Swagger UI, click **Authorize** and paste the token from /login.",
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid", content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Token missing, malformed, tampered with, or expired",
                    content = @Content)
    })
    public ResponseEntity<Void> validateToken(
            @Parameter(hidden = true)   // documented via the bearerAuth security scheme instead
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        // Expect: Authorization: Bearer <token>
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        return authService.validateToken(token)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
