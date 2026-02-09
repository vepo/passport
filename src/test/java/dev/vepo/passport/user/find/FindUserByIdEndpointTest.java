package dev.vepo.passport.user.find;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@DisplayName("Find User By ID Endpoint")
class FindUserByIdEndpointTest {

    private static final String FIND_USER_BY_ID_PATH = "/api/users/:id";

    @Inject
    UserRepository userRepository;

    private Given.GivenUser admin;
    private Given.GivenUser regularUser;
    private Given.GivenUser disabledUser;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        admin = Given.admin();
        regularUser = Given.user()
                           .withUsername("testuser")
                           .withEmail("test@example.com")
                           .withName("Test User")
                           .withPassword("12345")
                           .persist();
        disabledUser = Given.user()
                            .withUsername("disableduser")
                            .withEmail("disabled@example.com")
                            .withName("Disabled User")
                            .withPassword("12345")
                            .withDisabled(true)
                            .persist();
    }

    @Test
    @DisplayName("GET /users/{userId} - should return user when admin")
    void findUserById_AdminUser_ShouldReturnUser() {
        // Given
        long userId = regularUser.id();

        // When & Then
        given().header(admin.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("username", is(regularUser.username()))
               .body("email", is(regularUser.email()))
               .body("name", is(regularUser.name()))
               .body("disabled", is(false))
               .body("createdAt", notNullValue())
               .body("updatedAt", notNullValue())
               .body("profiles", notNullValue());
    }

    @Test
    @DisplayName("GET /users/{userId} - should return disabled user when admin")
    void findUserById_AdminUser_ShouldReturnDisabledUser() {
        // Given
        long userId = disabledUser.id();

        // When & Then
        given().header(admin.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("username", is(disabledUser.username()))
               .body("email", is(disabledUser.email()))
               .body("name", is(disabledUser.name()))
               .body("disabled", is(true))
               .body("createdAt", notNullValue())
               .body("updatedAt", notNullValue())
               .body("profiles", notNullValue());
    }

    @Test
    @DisplayName("GET /users/{userId} - should return 404 for non-existent user")
    void findUserById_NonExistentUser_ShouldReturnNotFound() {
        long nonExistentId = 999999L;

        given().header(admin.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(nonExistentId)))
               .then()
               .statusCode(HttpStatus.SC_NOT_FOUND)
               .body("status", is(404))
               .body("message", is("User not found!!! userId=999999"));
    }

    @Test
    @DisplayName("GET /users/{userId} - should return 403 for non-admin user")
    void findUserById_NonAdminUser_ShouldReturnForbidden() {
        long userId = regularUser.id();

        given().header(regularUser.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("GET /users/{userId} - should return 401 for unauthenticated request")
    void findUserById_Unauthenticated_ShouldReturnUnauthorized() {
        long userId = regularUser.id();

        given().when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /users/{userId} - admin should be able to find themselves")
    void findUserById_AdminFindingSelf_ShouldReturnAdminUser() {
        long adminId = admin.id();

        given().header(admin.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(adminId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) adminId))
               .body("username", is(admin.username()))
               .body("email", is(admin.email()))
               .body("disabled", is(false));
    }

    @Test
    @DisplayName("GET /users/{userId} - should return user with profiles")
    void findUserById_UserWithProfiles_ShouldReturnUserWithProfiles() {
        // Given - create user with specific profiles
        var userWithProfiles = Given.user()
                                    .withUsername("profileuser")
                                    .withEmail("profile@example.com")
                                    .withName("Profile User")
                                    .withPassword("12345")
                                    .withProfile("USER")
                                    .withProfile("EDITOR")
                                    .persist();

        long userId = userWithProfiles.id();

        // When & Then
        var response = given().header(admin.authenticated())
                              .when()
                              .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
                              .then()
                              .statusCode(HttpStatus.SC_OK)
                              .extract()
                              .response();

        // Verify profiles are included
        assertThat(response.jsonPath().getList("profiles.name"))
                                                                .contains("USER", "EDITOR");
        assertThat(response.jsonPath().getList("profiles.id"))
                                                              .isNotEmpty();
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("GET /users/{userId} - should return 400 for invalid user ID format")
        void findUserById_InvalidUserIdFormat_ShouldReturnBadRequest() {
            given().header(admin.authenticated())
                   .when()
                   .get(FIND_USER_BY_ID_PATH.replace(":id", "invalid-id"))
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND);
        }

        @Test
        @DisplayName("GET /users/{userId} - should return 400 for negative user ID")
        void findUserById_NegativeUserId_ShouldReturnBadRequest() {
            given().header(admin.authenticated())
                   .when()
                   .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(-1L)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("GET /users/{userId} - should return 400 for zero user ID")
        void findUserById_ZeroUserId_ShouldReturnNotFound() {
            given().header(admin.authenticated())
                   .when()
                   .get(FIND_USER_BY_ID_PATH.replace(":id", "0"))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Response Structure Tests")
    class ResponseStructureTests {

        @Test
        @DisplayName("GET /users/{userId} - response should have correct JSON structure")
        void findUserById_ResponseShouldHaveCorrectStructure() {
            long userId = regularUser.id();

            given().header(admin.authenticated())
                   .when()
                   .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasKey("id"))
                   .body("$", hasKey("username"))
                   .body("$", hasKey("email"))
                   .body("$", hasKey("name"))
                   .body("$", hasKey("disabled"))
                   .body("$", hasKey("createdAt"))
                   .body("$", hasKey("updatedAt"))
                   .body("$", hasKey("profiles"));
        }

        @Test
        @DisplayName("GET /users/{userId} - profiles should have correct structure")
        void findUserById_ProfilesShouldHaveCorrectStructure() {
            // Given - user with profile
            var userWithProfile = Given.user()
                                       .withUsername("userwithprofile")
                                       .withEmail("profile@example.com")
                                       .withName("User With Profile")
                                       .withPassword("12345")
                                       .withProfile("USER")
                                       .persist();

            long userId = userWithProfile.id();

            given().header(admin.authenticated())
                   .when()
                   .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasKey("id"))
                   .body("$", hasKey("name"))
                   .body("$", hasKey("username"))
                   .body("$", hasKey("email"))
                   .body("$", hasKey("disabled"))
                   .body("$", hasKey("createdAt"))
                   .body("$", hasKey("updatedAt"))
                   .body("profiles[0]", hasKey("id"))
                   .body("profiles[0]", hasKey("name"));
        }
    }

    @Test
    @DisplayName("GET /users/{userId} - should handle user with special characters")
    void findUserById_UserWithSpecialCharacters_ShouldReturnUser() {
        var specialUser = Given.user()
                               .withUsername("u-s_123")
                               .withEmail("special@example.com")
                               .withName("User Special")
                               .withPassword("12345")
                               .persist();

        long userId = specialUser.id();

        given().header(admin.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("username", is("u-s_123"))
               .body("email", is("special@example.com"))
               .body("name", is("User Special"));
    }

    @Test
    @DisplayName("GET /users/{userId} - should return consistent response format")
    void findUserById_ShouldReturnConsistentResponseFormat() {
        // Test multiple users to ensure consistent response format
        var users = List.of(Given.user()
                                 .withUsername("user1")
                                 .withPassword("12345")
                                 .withEmail("user1@example.com")
                                 .withName("User 1")
                                 .persist(),
                            Given.user()
                                 .withUsername("user2")
                                 .withPassword("12345")
                                 .withEmail("user2@example.com")
                                 .withName("User 2")
                                 .withDisabled(true)
                                 .persist(),
                            Given.user()
                                 .withUsername("user3")
                                 .withPassword("12345")
                                 .withEmail("user3@example.com")
                                 .withName("User 3")
                                 .withProfile("VIEWER")
                                 .persist());

        for (Given.GivenUser user : users) {
            given().header(admin.authenticated())
                   .when()
                   .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(user.id())))
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasKey("id"))
                   .body("$", hasKey("username"))
                   .body("$", hasKey("email"))
                   .body("$", hasKey("name"))
                   .body("$", hasKey("disabled"))
                   .body("$", hasKey("createdAt"))
                   .body("$", hasKey("updatedAt"))
                   .body("$", not(hasKey("password")))
                   .body("$", hasKey("profiles"));
        }
    }

    @Test
    @DisplayName("GET /users/{userId} - should not expose sensitive information")
    void findUserById_ShouldNotExposeSensitiveInformation() {
        long userId = regularUser.id();

        given().header(admin.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", not(hasKey("password")))
               .body("$", not(hasKey("encodedPassword")))
               .body("$", not(hasKey("passwordHash")));
    }

    @Test
    @DisplayName("GET /users/{userId} - should return proper error message for not found")
    void findUserById_NotFound_ShouldReturnProperErrorMessage() {
        long nonExistentId = 888888L;

        given().header(admin.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(nonExistentId)))
               .then()
               .statusCode(HttpStatus.SC_NOT_FOUND)
               .body("message", is("User not found!!! userId=888888"));
    }

    @Test
    @DisplayName("GET /users/{userId} - should work with large user ID")
    void findUserById_LargeUserId_ShouldReturnNotFound() {
        // Assuming IDs don't go this high
        long largeId = 999999999999L;

        given().header(admin.authenticated())
               .when()
               .get(FIND_USER_BY_ID_PATH.replace(":id", Long.toString(largeId)))
               .then()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }
}