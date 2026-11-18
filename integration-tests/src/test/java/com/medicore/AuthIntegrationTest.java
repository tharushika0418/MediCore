package com.medicore;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

/**
 * End-to-end tests for login and token validation.
 * <p>
 * Runs against the real stack through the API Gateway
 * (gateway -> auth-service -> auth-service-db). Start the containers first.
 */
class AuthIntegrationTest {

    /** Seeded by auth-service's data.sql. */
    private static final String TEST_EMAIL = "testuser@test.com";
    private static final String TEST_PASSWORD = "password123";

    /** A JWT is three Base64URL parts separated by dots: header.payload.signature */
    private static final String JWT_PATTERN = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = System.getProperty("gateway.url", "http://localhost:4004");
    }

    // ---------------------------------------------------------------- login

    @Test
    @DisplayName("POST /auth/login with valid credentials returns 200 and a JWT")
    void shouldReturnOkWithValidToken() {
        given()
                .contentType(ContentType.JSON)
                .body(loginBody(TEST_EMAIL, TEST_PASSWORD))
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .body("token", not(emptyOrNullString()))
                .body("token", matchesPattern(JWT_PATTERN));
    }

    @Test
    @DisplayName("POST /auth/login with a wrong password returns 401")
    void shouldReturnUnauthorizedOnWrongPassword() {
        given()
                .contentType(ContentType.JSON)
                .body(loginBody(TEST_EMAIL, "wrongpassword"))
        .when()
                .post("/auth/login")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST /auth/login with an unknown email returns 401 (same as a wrong password)")
    void shouldReturnUnauthorizedOnUnknownEmail() {
        given()
                .contentType(ContentType.JSON)
                .body(loginBody("nobody@test.com", TEST_PASSWORD))
        .when()
                .post("/auth/login")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("POST /auth/login with invalid input returns 400")
    void shouldReturnBadRequestOnInvalidInput() {
        given()
                .contentType(ContentType.JSON)
                .body(loginBody("not-an-email", "short"))
        .when()
                .post("/auth/login")
        .then()
                .statusCode(400);
    }

    // ------------------------------------------------------------- validate

    @Test
    @DisplayName("GET /auth/validate with a token from login returns 200")
    void shouldValidateTokenFromLogin() {
        String token = loginAndGetToken();

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/auth/validate")
        .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("GET /auth/validate with a tampered token returns 401")
    void shouldRejectTamperedToken() {
        String token = loginAndGetToken();
        // Change the FIRST character of the signature (all 6 bits are significant).
        // The last character is a bad choice: it carries 2 unused padding bits, so
        // some changes there decode to the same signature bytes.
        int sigStart = token.lastIndexOf('.') + 1;
        char first = token.charAt(sigStart);
        String tampered = token.substring(0, sigStart)
                + (first == 'A' ? 'B' : 'A')
                + token.substring(sigStart + 1);

        given()
                .header("Authorization", "Bearer " + tampered)
        .when()
                .get("/auth/validate")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("GET /auth/validate without a token returns 401")
    void shouldRejectMissingToken() {
        given()
        .when()
                .get("/auth/validate")
        .then()
                .statusCode(401);
    }

    // -------------------------------------------------------------- helpers

    private static String loginBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private static String loginAndGetToken() {
        return given()
                .contentType(ContentType.JSON)
                .body(loginBody(TEST_EMAIL, TEST_PASSWORD))
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }
}
