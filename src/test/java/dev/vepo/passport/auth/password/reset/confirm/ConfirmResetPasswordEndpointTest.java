package dev.vepo.passport.auth.password.reset.confirm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("Confirm Reset Password API Endpoint Tests")
class ConfirmResetPasswordEndpointTest {

    private static final String CONFIRM_RESET_ENDPOINT = "/api/auth/reset";
    private static final String TOKEN = "valid-reset-token-123";
    private static final String RECOVERY_PASSWORD = "recoveryPassword123";
    private static final String NEW_PASSWORD = "newSecurePassword456";
    private static final String INVALID_TOKEN = "invalid-token-456";
    private static final String EXPIRED_TOKEN = "expired-token-789";
    private static final String ALREADY_USED_TOKEN = "used-token-101";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Nested
    @DisplayName("Successful Password Reset Scenarios")
    class SuccessfulResetTests {

        @Test
        @DisplayName("Should successfully reset password with valid token and recovery password")
        void resetPassword_WithValidToken_ReturnsOk() {
            // Arrange
            var user = Given.user()
                            .withName("Reset Test User")
                            .withUsername("resetuser")
                            .withEmail("reset@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            // Verify new password works
            given().contentType(ContentType.JSON)
                   .body(loginRequest("reset@passport.vepo.dev", NEW_PASSWORD))
                   .when()
                   .post("/api/auth/login")
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should NOT allow login with old password after reset")
        void resetPassword_AfterReset_OldPasswordInvalid() {
            // Arrange
            var oldPassword = "oldPassword123";
            var user = Given.user()
                            .withName("Reset Old Password Test")
                            .withUsername("resetoldpass")
                            .withEmail("resetold@passport.vepo.dev")
                            .withPassword(oldPassword)
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act - Reset password
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            // Assert - Old password should not work
            given().contentType(ContentType.JSON)
                   .body(loginRequest("resetold@passport.vepo.dev", oldPassword))
                   .when()
                   .post("/api/auth/login")
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should mark token as used after successful reset")
        void resetPassword_SuccessfulReset_TokenMarkedAsUsed() {
            // Arrange
            var user = Given.user()
                            .withName("Token Usage Test")
                            .withUsername("tokenuser")
                            .withEmail("token@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act - First reset should succeed
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            // Assert - Second attempt with same token should fail
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, "anotherNewPassword"))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Token Validation Failure Scenarios")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return NOT_FOUND with invalid token")
        void resetPassword_WithInvalidToken_ReturnsNotFound() {
            // Arrange - Create user but no reset token
            var user = Given.user()
                            .withName("Invalid Token Test")
                            .withUsername("invalidtoken")
                            .withEmail("invalidtoken@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(INVALID_TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("Should return NOT_FOUND with incorrect recovery password")
        void resetPassword_WithIncorrectRecoveryPassword_ReturnsNotFound() {
            // Arrange
            var user = Given.user()
                            .withName("Wrong Recovery Test")
                            .withUsername("wrongrecovery")
                            .withEmail("wrongrecovery@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, "wrongRecoveryPassword", NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("Should return NOT_FOUND with expired token")
        void resetPassword_WithExpiredToken_ReturnsNotFound() {
            // Arrange
            var user = Given.user()
                            .withName("Expired Token Test")
                            .withUsername("expiredtoken")
                            .withEmail("expired@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(EXPIRED_TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now().minus(2, ChronoUnit.DAYS)) // 2 days old, expired
                 .withUsed(false)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(EXPIRED_TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("Should return NOT_FOUND with already used token")
        void resetPassword_WithAlreadyUsedToken_ReturnsNotFound() {
            // Arrange
            var user = Given.user()
                            .withName("Used Token Test")
                            .withUsername("usedtoken")
                            .withEmail("used@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(ALREADY_USED_TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(true) // Already used
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(ALREADY_USED_TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("Should return NOT_FOUND for deleted user")
        void resetPassword_ForDeletedUser_ReturnsNotFound() {
            // Arrange
            var deletedUser = Given.user()
                                   .withName("Deleted User Test")
                                   .withUsername("deleteduser")
                                   .withEmail("deleted@passport.vepo.dev")
                                   .withPassword("oldPassword123")
                                   .withDisabled(true)
                                   .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(deletedUser)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return BAD_REQUEST when token is null")
        void resetPassword_WithNullToken_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("""
                         {
                            "recoveryPassword": "%s",
                            "newPassword": "%s"
                         }
                         """.formatted(RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("reset.request.token"))
                   .body("violations[0].message", is("must not be empty"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when token is empty")
        void resetPassword_WithEmptyToken_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest("", RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("reset.request.token"))
                   .body("violations[0].message", is("must not be empty"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when recovery password is null")
        void resetPassword_WithNullRecoveryPassword_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("""
                         {
                            "token": "%s",
                            "newPassword": "%s"
                         }
                         """.formatted(TOKEN, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("reset.request.recoveryPassword"))
                   .body("violations[0].message", is("must not be empty"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when new password is null")
        void resetPassword_WithNullNewPassword_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("""
                         {
                            "token": "%s",
                            "recoveryPassword": "%s"
                         }
                         """.formatted(TOKEN, RECOVERY_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("reset.request.newPassword"))
                   .body("violations[0].message", is("must not be empty"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when new password is too short")
        void resetPassword_WithShortNewPassword_ReturnsBadRequest() {
            // Arrange
            var user = Given.user()
                            .withName("Short Password Test")
                            .withUsername("shortpass")
                            .withEmail("short@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, "123"))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("reset.request.newPassword"))
                   .body("violations[0].message", is("size must be between 8 and 20"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with malformed JSON")
        void resetPassword_WithMalformedJson_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("{ malformed json }")
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return UNSUPPORTED_MEDIA_TYPE without Content-Type header")
        void resetPassword_WithoutContentType_ReturnsUnsupportedMediaType() {
            given().body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    @Nested
    @DisplayName("New Password Validation Tests")
    class NewPasswordValidationTests {

        @Test
        @DisplayName("Should accept strong new password")
        void resetPassword_WithStrongNewPassword_Succeeds() {
            // Arrange
            var strongPassword = "Str0ngP@ssw0rd!2024";
            var user = Given.user()
                            .withName("Strong Password Test")
                            .withUsername("strongpass")
                            .withEmail("strong@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, strongPassword))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should accept new password with special characters")
        void resetPassword_WithSpecialCharacters_Succeeds() {
            // Arrange
            var specialPassword = "P@$$w0rd!№;%:?*()_+";
            var user = Given.user()
                            .withName("Special Chars Test")
                            .withUsername("specialchars")
                            .withEmail("special@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, specialPassword))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should accept long new password")
        void resetPassword_WithLongPassword_Succeeds() {
            // Arrange
            var longPassword = "LongPassword8!@#%^()";
            var user = Given.user()
                            .withName("Long Password Test")
                            .withUsername("longpassword")
                            .withEmail("long@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, longPassword))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle multiple reset attempts for different users")
        void resetPassword_MultipleUsersDifferentTokens_Success() {
            // Arrange - User 1
            var user1 = Given.user()
                             .withName("Multi User 1")
                             .withUsername("multiuser1")
                             .withEmail("multi1@passport.vepo.dev")
                             .withPassword("oldPassword123")
                             .persist();

            Given.resetPassword()
                 .withToken("token-user-1")
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user1)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Arrange - User 2
            var user2 = Given.user()
                             .withName("Multi User 2")
                             .withUsername("multiuser2")
                             .withEmail("multi2@passport.vepo.dev")
                             .withPassword("oldPassword456")
                             .persist();

            Given.resetPassword()
                 .withToken("token-user-2")
                 .withPassword("differentRecoveryPass")
                 .withUser(user2)
                 .withRequestedAt(Instant.now())
                 .withUsed(false)
                 .persist();

            // Act & Assert - Both should succeed
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest("token-user-1", RECOVERY_PASSWORD, "newPass1"))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest("token-user-2", "differentRecoveryPass", "newPass2"))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should handle token with exact 24-hour validity")
        void resetPassword_TokenExactly24HoursOld_Succeeds() {
            // Arrange
            var user = Given.user()
                            .withName("24 Hour Test")
                            .withUsername("24hour")
                            .withEmail("24hour@passport.vepo.dev")
                            .withPassword("oldPassword123")
                            .persist();

            Given.resetPassword()
                 .withToken(TOKEN)
                 .withPassword(RECOVERY_PASSWORD)
                 .withUser(user)
                 .withRequestedAt(Instant.now().minus(23, ChronoUnit.HOURS).minus(59, ChronoUnit.MINUTES))
                 .withUsed(false)
                 .persist();

            // Act & Assert - Should still be valid (just under 24 hours)
            given().contentType(ContentType.JSON)
                   .body(resetPasswordRequest(TOKEN, RECOVERY_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CONFIRM_RESET_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * Helper method to create a reset password request JSON body.
     */
    private String resetPasswordRequest(String token, String recoveryPassword, String newPassword) {
        return """
               {
                   "token": "%s",
                   "recoveryPassword": "%s",
                   "newPassword": "%s"
               }
               """.formatted(token, recoveryPassword, newPassword);
    }

    /**
     * Helper method to create a login request JSON body.
     */
    private String loginRequest(String email, String password) {
        return """
               {
                   "email": "%s",
                   "password": "%s"
               }
               """.formatted(email, password);
    }
}