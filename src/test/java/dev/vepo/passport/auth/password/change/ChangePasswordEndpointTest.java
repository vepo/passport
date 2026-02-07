package dev.vepo.passport.auth.password.change;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("Change Password API Endpoint Tests")
class ChangePasswordEndpointTest {

    private static final String CHANGE_PASSWORD_ENDPOINT = "/api/auth/change-password";
    private static final String CURRENT_PASSWORD = "currentPassword123";
    private static final String NEW_PASSWORD = "newSecurePassword456";
    private static final String WRONG_PASSWORD = "wrongPassword123";
    private static final String STRONG_NEW_PASSWORD = "Str0ngP@ssw0rd!2024";

    @Nested
    @DisplayName("Authentication Required")
    class AuthenticationRequiredTests {

        @Test
        @DisplayName("Should return UNAUTHORIZED when accessing endpoint without authentication")
        void changePassword_WithoutAuthentication_ReturnsUnauthorized() {
            given().contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Authenticated User Scenarios")
    class AuthenticatedUserTests {

        @Test
        @DisplayName("Should successfully change password with valid credentials")
        void changePassword_WithValidCredentials_ReturnsOk() {
            // Arrange
            var userAuth = Given.user()
                                .withName("Test User")
                                .withUsername("testuser")
                                .withEmail("testuser@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            // Act & Assert
            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should return FORBIDDEN when current password is incorrect")
        void changePassword_WithIncorrectCurrentPassword_ReturnsForbidden() {
            // Arrange
            var userAuth = Given.user()
                                .withName("Test User")
                                .withUsername("testuser2")
                                .withEmail("testuser2@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            // Act & Assert - using wrong current password
            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(WRONG_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_FORBIDDEN);
        }

        @Test
        @DisplayName("Should return FORBIDDEN when user account is deleted")
        void changePassword_WithDeletedUser_ReturnsForbidden() {
            // Arrange
            var deletedUserAuth = Given.user()
                                       .withName("Deleted User")
                                       .withUsername("deleteduser")
                                       .withEmail("deleteduser@passport.vepo.dev")
                                       .withPassword(CURRENT_PASSWORD)
                                       .withDeleted(true)
                                       .persist()
                                       .authenticated();

            // Act & Assert
            given().header(deletedUserAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_FORBIDDEN);
        }

        @Test
        @DisplayName("Should be able to login with new password after successful change")
        void changePassword_ThenLoginWithNewPassword_Succeeds() {
            // Arrange
            var user = Given.user()
                            .withName("Password Change User")
                            .withUsername("passchangeuser")
                            .withEmail("passchange@passport.vepo.dev")
                            .withPassword(CURRENT_PASSWORD)
                            .persist();

            var userAuth = user.authenticated();

            // Act - Change password
            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            // Assert - Can login with new password
            given().contentType(ContentType.JSON)
                   .body(loginRequest("passchange@passport.vepo.dev", NEW_PASSWORD))
                   .when()
                   .post("/api/auth/login")
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should NOT be able to login with old password after successful change")
        void changePassword_ThenLoginWithOldPassword_Fails() {
            // Arrange
            var user = Given.user()
                            .withName("Old Password Test User")
                            .withUsername("oldpassworduser")
                            .withEmail("oldpassword@passport.vepo.dev")
                            .withPassword(CURRENT_PASSWORD)
                            .persist();

            var userAuth = user.authenticated();

            // Act - Change password
            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            // Assert - Cannot login with old password
            given().contentType(ContentType.JSON)
                   .body(loginRequest("oldpassword@passport.vepo.dev", CURRENT_PASSWORD))
                   .when()
                   .post("/api/auth/login")
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return BAD_REQUEST when current password is null")
        void changePassword_WithNullCurrentPassword_ReturnsBadRequest() {
            var userAuth = Given.user()
                                .withName("Validation User")
                                .withUsername("validationuser")
                                .withEmail("validation@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                            "newPassword": "%s"
                         }
                         """.formatted(NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when new password is null")
        void changePassword_WithNullNewPassword_ReturnsBadRequest() {
            var userAuth = Given.user()
                                .withName("Validation User 2")
                                .withUsername("validationuser2")
                                .withEmail("validation2@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                            "currentPassword": "%s"
                         }
                         """.formatted(CURRENT_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when current password is empty")
        void changePassword_WithEmptyCurrentPassword_ReturnsBadRequest() {
            var userAuth = Given.user()
                                .withName("Validation User 3")
                                .withUsername("validationuser3")
                                .withEmail("validation3@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest("", NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when new password is empty")
        void changePassword_WithEmptyNewPassword_ReturnsBadRequest() {
            var userAuth = Given.user()
                                .withName("Validation User 4")
                                .withUsername("validationuser4")
                                .withEmail("validation4@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, ""))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when new password is too short")
        void changePassword_WithShortNewPassword_ReturnsBadRequest() {
            var userAuth = Given.user()
                                .withName("Validation User 5")
                                .withUsername("validationuser5")
                                .withEmail("validation5@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, "123"))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("change.request.newPassword"))
                   .body("violations[0].message", is("Password must be at least 8 characters long"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when new password equals current password")
        void changePassword_WithSamePassword_ReturnsBadRequest() {
            var userAuth = Given.user()
                                .withName("Validation User 6")
                                .withUsername("validationuser6")
                                .withEmail("validation6@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, CURRENT_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("change.request"))
                   .body("violations[0].message", is("New password must be different from current password"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with malformed JSON")
        void changePassword_WithMalformedJson_ReturnsBadRequest() {
            var userAuth = Given.user()
                                .withName("Validation User 7")
                                .withUsername("validationuser7")
                                .withEmail("validation7@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body("{ malformed json }")
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return UNSUPPORTED_MEDIA_TYPE without Content-Type header")
        void changePassword_WithoutContentType_ReturnsUnsupportedMediaType() {
            var userAuth = Given.user()
                                .withName("Validation User 8")
                                .withUsername("validationuser8")
                                .withEmail("validation8@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            given().header(userAuth)
                   .body(changePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    @Nested
    @DisplayName("Password Security Tests")
    class PasswordSecurityTests {

        @Test
        @DisplayName("Should successfully change password with strong new password")
        void changePassword_WithStrongNewPassword_ReturnsOk() {
            // Arrange
            var userAuth = Given.user()
                                .withName("Security User")
                                .withUsername("securityuser")
                                .withEmail("security@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            // Act & Assert
            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, STRONG_NEW_PASSWORD))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should accept passwords with special characters")
        void changePassword_WithSpecialCharacters_ReturnsOk() {
            // Arrange
            var specialPassword = "P@$$w0rd!№;%:?*()_+";
            var userAuth = Given.user()
                                .withName("Special Char User")
                                .withUsername("specialuser")
                                .withEmail("special@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            // Act & Assert
            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, specialPassword))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }

        @Test
        @DisplayName("Should accept long passwords")
        void changePassword_WithLongPassword_ReturnsOk() {
            // Arrange
            var longPassword = "VeryLongPassword1234567890!@#$%^&*()";
            var userAuth = Given.user()
                                .withName("Long Password User")
                                .withUsername("longpassuser")
                                .withEmail("longpass@passport.vepo.dev")
                                .withPassword(CURRENT_PASSWORD)
                                .persist()
                                .authenticated();

            // Act & Assert
            given().header(userAuth)
                   .contentType(ContentType.JSON)
                   .body(changePasswordRequest(CURRENT_PASSWORD, longPassword))
                   .when()
                   .post(CHANGE_PASSWORD_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * Helper method to create a change password request JSON body.
     */
    private String changePasswordRequest(String currentPassword, String newPassword) {
        return """
               {
                   "currentPassword": "%s",
                   "newPassword": "%s"
               }
               """.formatted(currentPassword, newPassword);
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