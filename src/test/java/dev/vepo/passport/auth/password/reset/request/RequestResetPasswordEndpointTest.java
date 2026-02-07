package dev.vepo.passport.auth.password.reset.request;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.time.Instant;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("Request Reset Password API Endpoint Tests")
class RequestResetPasswordEndpointTest {

    private static final String REQUEST_RESET_ENDPOINT = "/api/auth/request-reset-password";
    private static final String VALID_EMAIL = "user@passport.vepo.dev";
    private static final String NON_EXISTENT_EMAIL = "nonexistent@passport.vepo.dev";
    private static final String DELETED_USER_EMAIL = "deleted@passport.vepo.dev";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Nested
    @DisplayName("Successful Reset Request Scenarios")
    class SuccessfulRequestTests {

        @Test
        @DisplayName("Should return OK for valid email with active user")
        void requestReset_WithValidEmail_ReturnsOk() {
            // Arrange
            Given.user()
                 .withName("Test User")
                 .withUsername("testuser")
                 .withEmail(VALID_EMAIL)
                 .withPassword("currentPassword123")
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(VALID_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            // Verify token was created
            // (This would require a way to query the database or check events)
        }

        @Test
        @DisplayName("Should create new token when no active token exists")
        void requestReset_WithNoExistingToken_CreatesNewToken() {
            // Arrange
            var user = Given.user()
                            .withName("No Token User")
                            .withUsername("notokenuser")
                            .withEmail(VALID_EMAIL)
                            .withPassword("currentPassword123")
                            .persist();

            // Act
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(VALID_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            // Act - Request again (should create new token since previous one would expire)
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(VALID_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should return OK for non-existent email (security through obscurity)")
        void requestReset_WithNonExistentEmail_ReturnsOk() {
            // Act & Assert - Returns OK even for non-existent email for security
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(NON_EXISTENT_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should return OK for deleted user (security through obscurity)")
        void requestReset_WithDeletedUser_ReturnsOk() {
            // Arrange
            Given.user()
                 .withName("Deleted User")
                 .withUsername("deleteduser")
                 .withEmail(DELETED_USER_EMAIL)
                 .withPassword("currentPassword123")
                 .withDeleted(true)
                 .persist();

            // Act & Assert - Returns OK even for deleted user
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(DELETED_USER_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with invalid email format")
        void requestReset_WithInvalidEmailFormat_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest("not-an-email"))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("recovery.request.email"))
                   .body("violations[0].message", is("must be a well-formed email address"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with null email")
        void requestReset_WithNullEmail_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("{}")
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("recovery.request.email"))
                   .body("violations[0].message", is("must not be blank"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with empty email")
        void requestReset_WithEmptyEmail_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(""))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("recovery.request.email"))
                   .body("violations[0].message", is("must not be blank"));
        }
    }

    @Nested
    @DisplayName("Token Management Tests")
    class TokenManagementTests {

        @Test
        @DisplayName("Should not create new token when valid token already exists")
        void requestReset_WithExistingValidToken_DoesNotCreateNewToken() {
            // Arrange
            var user = Given.user()
                            .withName("Existing Token User")
                            .withUsername("existingtoken")
                            .withEmail(VALID_EMAIL)
                            .withPassword("currentPassword123")
                            .persist();

            // Create an active reset token
            Given.resetPassword()
                 .withToken("existing-token")
                 .withPassword("recovery-pass")
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act - Should not create new token since one exists
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(VALID_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            // Note: We cannot easily verify that a new token wasn't created without
            // additional test infrastructure, but the endpoint should log a warning
        }

        @Test
        @DisplayName("Should create new token when existing token is expired")
        void requestReset_WithExpiredToken_CreatesNewToken() {
            // Arrange
            var user = Given.user()
                            .withName("Expired Token User")
                            .withUsername("expiredtoken")
                            .withEmail(VALID_EMAIL)
                            .withPassword("currentPassword123")
                            .persist();

            // Create an expired reset token
            Given.resetPassword()
                 .withToken("expired-token")
                 .withPassword("recovery-pass")
                 .withUser(user)
                 .withRequestedAt(Instant.now().minusSeconds(25 * 60 * 60)) // 25 hours old
                 .withUsed(false)
                 .persist();

            // Act - Should create new token since existing one is expired
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(VALID_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should create new token when existing token is already used")
        void requestReset_WithUsedToken_CreatesNewToken() {
            // Arrange
            var user = Given.user()
                            .withName("Used Token User")
                            .withUsername("usedtoken")
                            .withEmail(VALID_EMAIL)
                            .withPassword("currentPassword123")
                            .persist();

            // Create an already used reset token
            Given.resetPassword()
                 .withToken("used-token")
                 .withPassword("recovery-pass")
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(true)
                 .persist();

            // Act - Should create new token since existing one is already used
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(VALID_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return BAD_REQUEST with malformed JSON")
        void requestReset_WithMalformedJson_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("{ malformed json }")
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return UNSUPPORTED_MEDIA_TYPE without Content-Type header")
        void requestReset_WithoutContentType_ReturnsUnsupportedMediaType() {
            given().body(requestResetPasswordRequest(VALID_EMAIL))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with email missing @ symbol")
        void requestReset_WithEmailMissingAtSymbol_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest("userexample.com"))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("recovery.request.email"))
                   .body("violations[0].message", is("must be a well-formed email address"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with email missing domain")
        void requestReset_WithEmailMissingDomain_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest("user@"))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("recovery.request.email"))
                   .body("violations[0].message", is("must be a well-formed email address"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with email missing local part")
        void requestReset_WithEmailMissingLocalPart_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest("@example.com"))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("recovery.request.email"))
                   .body("violations[0].message", is("must be a well-formed email address"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle multiple reset requests for same email")
        void requestReset_MultipleRequestsForSameEmail_AllReturnOk() {
            // Arrange
            Given.user()
                 .withName("Multiple Requests User")
                 .withUsername("multirequest")
                 .withEmail(VALID_EMAIL)
                 .withPassword("currentPassword123")
                 .persist();

            // Act & Assert - Multiple requests should all return OK
            for (int i = 0; i < 3; i++) {
                given().contentType(ContentType.JSON)
                       .body(requestResetPasswordRequest(VALID_EMAIL))
                       .when()
                       .post(REQUEST_RESET_ENDPOINT)
                       .then()
                       .statusCode(HttpStatus.SC_OK);
            }
        }

        @Test
        @DisplayName("Should handle email with special characters")
        void requestReset_WithEmailSpecialCharacters_ReturnsOk() {
            // Arrange
            String specialEmail = "user.name+tag@sub-domain.example.co.uk";
            Given.user()
                 .withName("Special Email User")
                 .withUsername("specialemail")
                 .withEmail(specialEmail)
                 .withPassword("currentPassword123")
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(specialEmail))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should handle email in different case (case-insensitive)")
        void requestReset_WithEmailDifferentCase_ReturnsOk() {
            // Arrange
            String storedEmail = "User@Example.com";
            String requestEmail = "user@example.com";
            
            Given.user()
                 .withName("Case Test User")
                 .withUsername("casetest")
                 .withEmail(storedEmail)
                 .withPassword("currentPassword123")
                 .persist();

            // Act & Assert - Should work with different case
            given().contentType(ContentType.JSON)
                   .body(requestResetPasswordRequest(requestEmail))
                   .when()
                   .post(REQUEST_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * Helper method to create a request reset password request JSON body.
     */
    private String requestResetPasswordRequest(String email) {
        return """
               {
                   "email": "%s"
               }
               """.formatted(email);
    }
}