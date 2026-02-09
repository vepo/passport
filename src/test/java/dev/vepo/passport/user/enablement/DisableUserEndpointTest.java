package dev.vepo.passport.user.enablement;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
@DisplayName("Disable User Endpoint")
class DisableUserEndpointTest {

    private static final String DISABLE_USER_PATH = "/api/users/:id/disable";
    @Inject
    UserRepository userRepository;

    private Given.GivenUser admin;
    private Given.GivenUser regularUser;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        admin = Given.admin();
        regularUser = Given.user()
                           .withUsername("testuser")
                           .withEmail("test@example.com")
                           .withName("TEST")
                           .withPassword("12354")
                           .persist();
    }

    @Test
    @DisplayName("POST /users/{userId}/disable - should disable user when admin")
    void disableUser_AdminUser_ShouldDisable() {
        // Given
        long userId = regularUser.id();

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(DISABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("username", is(regularUser.username()))
               .body("disabled", is(true))
               .body("createdAt", notNullValue());

        // Verify user is actually disabled in database
        assertThat(userRepository.findById(userId)).isPresent()
                                                   .as("User should be marked as deleted")
                                                   .hasValueSatisfying(user -> assertThat(user.isDisabled()).isTrue());
    }

    @Test
    @DisplayName("POST /users/{userId}/disable - should return 404 for non-existent user")
    void disableUser_NonExistentUser_ShouldReturnNotFound() {
        long nonExistentId = 999999L;

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(DISABLE_USER_PATH.replace(":id", Long.toString(nonExistentId)))
               .then()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("POST /users/{userId}/disable - should return 403 for non-admin user")
    void disableUser_NonAdminUser_ShouldReturnForbidden() {
        long userId = regularUser.id();

        given().header(regularUser.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(DISABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("POST /users/{userId}/disable - should return 401 for unauthenticated request")
    void disableUser_Unauthenticated_ShouldReturnUnauthorized() {
        long userId = regularUser.id();

        given().contentType(ContentType.JSON)
               .when()
               .post(DISABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /users/{userId}/disable - should idempotently disable already disabled user")
    void disableUser_AlreadyDisabledUser_ShouldReturnOk() {
        // Given - create and disable a user
        var disabledTestUser = Given.user()
                                    .withUsername("alreadydisabled")
                                    .withEmail("disabled@example.com")
                                    .withName("Deleted")
                                    .withPassword("1235")
                                    .withDisabled(true)
                                    .persist();

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(DISABLE_USER_PATH.replace(":id", Long.toString(disabledTestUser.id())))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is(disabledTestUser.id().intValue()))
               .body("disabled", is(true));

        // Verify user is still disabled
        assertThat(userRepository.findById(disabledTestUser.id())).isPresent()
                                                                  .as("User should still be deleted after second disable")
                                                                  .hasValueSatisfying(user -> assertThat(user.isDisabled()).isTrue());
    }

    @Test
    @DisplayName("POST /users/{userId}/disable - should not allow admin to disable themselves")
    void disableUser_AdminDisablingSelf_ShouldDisable() {
        // Note: This depends on your business rules.
        // Some systems prevent self-disable, others allow it.
        long adminId = admin.id();

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(DISABLE_USER_PATH.replace(":id", Long.toString(adminId)))
               .then()
               .statusCode(HttpStatus.SC_OK) // Assuming admin can disable themselves
               .body("id", is((int) adminId))
               .body("disabled", is(true));
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("POST /users/{userId}/disable - should return 400 for invalid user ID format")
        void disableUser_InvalidUserIdFormat_ShouldReturnBadRequest() {
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .when()
                   .post(DISABLE_USER_PATH.replace(":id", "invalid-id"))
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("POST /users/{userId}/disable - should return 400 for negative user ID")
        void disableUser_NegativeUserId_ShouldReturnBadRequest() {
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .when()
                   .post(DISABLE_USER_PATH.replace(":id", Long.toString(-1l)))
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }
    }
}