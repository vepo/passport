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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.passport.shared.Given;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
@DisplayName("Enable User Endpoint")
class EnableUserEndpointTest {

    private static final Logger logger = LoggerFactory.getLogger(EnableUserEndpointTest.class);
    private static final String ENABLE_USER_PATH = "/api/users/:id/enable";
    private static final String DISABLE_USER_PATH = "/api/users/:id/disable";

    @Inject
    UserRepository userRepository;

    private Given.GivenUser admin;
    private Given.GivenUser disabledUser;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        admin = Given.admin();

        // Create and disable a user
        disabledUser = Given.user()
                            .withUsername("disableduser")
                            .withEmail("disabled@example.com")
                            .withName("Disabled User")
                            .withPassword("12354")
                            .withDisabled(true)
                            .persist();
    }

    @Test
    @DisplayName("POST /users/{userId}/enable - should enable disabled user when admin")
    void enableUser_AdminUser_ShouldEnable() {
        // Given
        long userId = disabledUser.id();

        // Verify user is initially disabled
        assertThat(userRepository.findById(userId))
                                                   .isPresent()
                                                   .hasValueSatisfying(user -> assertThat(user.isDisabled()).isTrue());

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("username", is(disabledUser.username()))
               .body("disabled", is(false))
               .body("createdAt", notNullValue());

        Given.clearDatabaseCache();
        // Verify user is actually enabled in database
        assertThat(userRepository.findById(userId))
                                                   .isPresent()
                                                   .hasValueSatisfying(user -> assertThat(user.isDisabled()).isFalse());
    }

    @Test
    @DisplayName("POST /users/{userId}/enable - should return 404 for non-existent user")
    void enableUser_NonExistentUser_ShouldReturnNotFound() {
        long nonExistentId = 999999L;

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(nonExistentId)))
               .then()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    @DisplayName("POST /users/{userId}/enable - should return 403 for non-admin user")
    void enableUser_NonAdminUser_ShouldReturnForbidden() {
        // Create a regular (non-admin) user
        var regularUser = Given.user()
                               .withUsername("regularuser")
                               .withEmail("regular@example.com")
                               .withName("Regular User")
                               .withPassword("12345")
                               .persist();

        long userId = disabledUser.id();

        given().header(regularUser.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("POST /users/{userId}/enable - should return 401 for unauthenticated request")
    void enableUser_Unauthenticated_ShouldReturnUnauthorized() {
        long userId = disabledUser.id();

        given().contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST /users/{userId}/enable - should idempotently enable already enabled user")
    void enableUser_AlreadyEnabledUser_ShouldReturnOk() {
        // Given - create an enabled user
        var enabledUser = Given.user()
                               .withUsername("alreadyenabled")
                               .withEmail("enabled@example.com")
                               .withName("Enabled User")
                               .withPassword("12345")
                               .persist();

        long userId = enabledUser.id();

        // Verify user is initially enabled
        assertThat(userRepository.findById(userId))
                                                   .isPresent()
                                                   .hasValueSatisfying(user -> assertThat(user.isDisabled()).isFalse());

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("disabled", is(false));

        // Verify user is still enabled
        assertThat(userRepository.findById(userId))
                                                   .isPresent()
                                                   .hasValueSatisfying(user -> assertThat(user.isDisabled()).isFalse());
    }

    @Test
    @DisplayName("POST /users/{userId}/enable - should allow admin to enable themselves if disabled")
    void enableUser_AdminEnablingSelf_ShouldEnable() {
        // Disable admin first
        Given.withTransaction(() -> {
            var adminEntity = userRepository.findByUsername(admin.username())
                                            .orElseThrow();
            adminEntity.setDisabled(true);
            userRepository.save(adminEntity);
        });

        long adminId = admin.id();

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(adminId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) adminId))
               .body("disabled", is(false));

        // Verify admin is enabled
        assertThat(userRepository.findById(adminId))
                                                    .isPresent()
                                                    .hasValueSatisfying(user -> assertThat(user.isDisabled()).isFalse());
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("POST /users/{userId}/enable - should preserve all user data when enabling")
        void enableUser_ShouldPreserveAllUserData() {
            // Create a user with all fields populated
            var completeUser = Given.user()
                                    .withUsername("completeuser")
                                    .withEmail("complete@example.com")
                                    .withName("Complete User")
                                    .withPassword("Complex@123")
                                    .withDisabled(true)
                                    .withProfile("USER")
                                    .withProfile("EDITOR")
                                    .persist();

            // Store original values
            var originalUser = userRepository.findById(completeUser.id()).orElseThrow();
            var originalUsername = originalUser.getUsername();
            var originalEmail = originalUser.getEmail();
            var originalName = originalUser.getName();
            var originalCreatedAt = originalUser.getCreatedAt();
            var originalProfileCount = originalUser.getProfiles().size();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .when()
                   .post(ENABLE_USER_PATH.replace(":id", Long.toString(completeUser.id())))
                   .then()
                   .statusCode(HttpStatus.SC_OK);

            Given.clearDatabaseCache();
            // Verify all data preserved
            var enabledUser = userRepository.findById(completeUser.id()).orElseThrow();
            logger.info("Found user={}", enabledUser);
            assertThat(enabledUser.isDisabled()).isFalse();
            assertThat(enabledUser.getUsername()).isEqualTo(originalUsername);
            assertThat(enabledUser.getEmail()).isEqualTo(originalEmail);
            assertThat(enabledUser.getName()).isEqualTo(originalName);
            assertThat(enabledUser.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(enabledUser.getProfiles()).hasSize(originalProfileCount);
        }
    }

    @Test
    @DisplayName("POST /users/{userId}/enable - should work after multiple disable/enable cycles")
    void enableUser_MultipleCycles_ShouldWork() {
        long userId = disabledUser.id();

        // First enable
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("disabled", is(false));

        // Disable again
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(DISABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("disabled", is(true));

        // Enable again (second cycle)
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("disabled", is(false));

        // Verify final state
        assertThat(userRepository.findById(userId))
                                                   .isPresent()
                                                   .hasValueSatisfying(user -> assertThat(user.isDisabled()).isFalse());
    }

    @Test
    @DisplayName("POST /users/{userId}/enable - should return proper response structure")
    void enableUser_ShouldReturnCompleteResponse() {
        long userId = disabledUser.id();

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .when()
               .post(ENABLE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("username", is(disabledUser.username()))
               .body("email", is(disabledUser.email()))
               .body("name", is(disabledUser.name()))
               .body("disabled", is(false))
               .body("createdAt", notNullValue())
               .body("updatedAt", notNullValue())
               .body("profiles", notNullValue());
    }
}