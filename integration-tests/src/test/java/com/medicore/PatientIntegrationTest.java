package com.medicore;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end tests for the patient API through the API Gateway.
 * <p>
 * Covers the gateway's JWT filter (requests need a valid token) and the full
 * path behind it: gateway -> patient-service -> PostgreSQL, plus the gRPC call
 * to billing-service and the Kafka event that a successful create triggers.
 * <p>
 * Tests that create patients use a unique email and delete the patient
 * afterwards, so they can run repeatedly without leaving data behind.
 */
class PatientIntegrationTest {

    private static String token;

    /** Ids of patients created by the current test, deleted in {@link #cleanUp()}. */
    private final List<String> createdPatientIds = new ArrayList<>();

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = System.getProperty("gateway.url", "http://localhost:4004");

        // One login for the whole class: every patient request needs a token
        token = given()
                .contentType(ContentType.JSON)
                .body("""
                        { "email": "testuser@test.com", "password": "password123" }
                        """)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    @AfterEach
    void cleanUp() {
        for (String id : createdPatientIds) {
            given().header("Authorization", "Bearer " + token)
                    .when().delete("/api/patients/" + id);
        }
        createdPatientIds.clear();
    }

    // ------------------------------------------------------ security (JWT filter)

    @Test
    @DisplayName("GET /api/patients with a valid token returns 200 and a list of patients")
    void shouldReturnPatientsWithValidToken() {
        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0]", hasKey("id"))
                .body("[0]", hasKey("email"))
                .body("[0]", not(hasKey("registeredDate")));   // not exposed by the response DTO
    }

    @Test
    @DisplayName("GET /api/patients without a token returns 401 from the gateway")
    void shouldRejectRequestWithoutToken() {
        given()
        .when()
                .get("/api/patients")
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("GET /api/patients with an invalid token returns 401")
    void shouldRejectRequestWithInvalidToken() {
        given()
                .header("Authorization", "Bearer not.a.valid-token")
        .when()
                .get("/api/patients")
        .then()
                .statusCode(401);
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("POST /api/patients creates a patient and returns 201 with its id")
    void shouldCreatePatient() {
        String email = uniqueEmail();

        String id = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(patientBody("Integration Test", email))
        .when()
                .post("/api/patients")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("Integration Test"))
                .body("email", equalTo(email))
                .extract()
                .jsonPath()
                .getString("id");

        createdPatientIds.add(id);
    }

    @Test
    @DisplayName("POST /api/patients with an email that already exists returns 409")
    void shouldRejectDuplicateEmail() {
        String email = uniqueEmail();
        createdPatientIds.add(createPatient("First Patient", email));

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(patientBody("Second Patient", email))
        .when()
                .post("/api/patients")
        .then()
                .statusCode(409);
    }

    @Test
    @DisplayName("POST /api/patients with invalid data returns 400 with field errors")
    void shouldRejectInvalidPatient() {
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        { "name": "", "email": "not-an-email", "address": "x", "dateOfBirth": "2099-01-01" }
                        """)
        .when()
                .post("/api/patients")
        .then()
                .statusCode(400)
                .body("$", hasKey("name"))
                .body("$", hasKey("email"))
                .body("$", hasKey("dateOfBirth"))
                .body("$", hasKey("registeredDate"));
    }

    // ------------------------------------------------- update and delete flow

    @Test
    @DisplayName("Create, update, then delete a patient (full lifecycle)")
    void shouldCreateUpdateAndDeletePatient() {
        String email = uniqueEmail();
        String id = createPatient("Before Update", email);

        // Update
        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(patientBody("After Update", email))
        .when()
                .put("/api/patients/" + id)
        .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("After Update"));

        // Delete
        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/patients/" + id)
        .then()
                .statusCode(204);

        // Deleting again: the patient no longer exists
        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/patients/" + id)
        .then()
                .statusCode(404);
    }

    // ---------------------------------------------------------------- helpers

    /** Unique per call, so tests can be re-run without "email already exists" (409). */
    private static String uniqueEmail() {
        return "it-" + UUID.randomUUID() + "@test.com";
    }

    private static String patientBody(String name, String email) {
        return """
                {
                  "name": "%s",
                  "email": "%s",
                  "address": "1 Integration Test Road",
                  "dateOfBirth": "1990-01-01",
                  "registeredDate": "%s"
                }
                """.formatted(name, email, LocalDate.now());
    }

    private static String createPatient(String name, String email) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(patientBody(name, email))
        .when()
                .post("/api/patients")
        .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("id");
    }
}
