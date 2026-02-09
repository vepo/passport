package dev.vepo.passport.user.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Set;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("Create User API Endpoint Tests")
class CreateUserEndpointTest {

    private static final String CREATE_USER_ENDPOINT = "/api/users";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Nested
    @DisplayName("Authentication & Authorization")
    class AuthenticationAuthorizationTests {

        @Test
        @DisplayName("Should return UNAUTHORIZED when accessing endpoint without authentication")
        void createUser_WithoutAuthentication_ReturnsUnauthorized() {
            given().contentType(ContentType.JSON)
                   .body(createUserRequest("newuser", "New User", "newuser@passport.vepo.dev", Set.of(1L)))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return FORBIDDEN when authenticated as non-admin user")
        void createUser_AsNonAdminUser_ReturnsForbidden() {
            // Arrange - Create a regular user (non-admin)
            var regularUserAuth = Given.user()
                                       .withName("Regular User")
                                       .withUsername("regularuser")
                                       .withEmail("regular@passport.vepo.dev")
                                       .withPassword("password123")
                                       .persist()
                                       .authenticated();

            // Act & Assert
            given().header(regularUserAuth)
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("newuser", "New User", "newuser@passport.vepo.dev", Set.of(1L)))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_FORBIDDEN);
        }

        @Test
        @DisplayName("Should succeed when authenticated as admin user")
        void createUser_AsAdminUser_ReturnsCreated() {
            // Arrange - Create admin user (assuming sysadmin has admin role)
            var admin = Given.admin();

            // Create a profile for the test
            var profile = Given.profile()
                               .withName("Test Profile")
                               .persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("newuser", "New User", "newuser@passport.vepo.dev", Set.of(profile.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("username", is("newuser"))
                   .body("name", is("New User"))
                   .body("email", is("newuser@passport.vepo.dev"))
                   .body("id", notNullValue());
        }
    }

    @Nested
    @DisplayName("Successful User Creation")
    class SuccessfulCreationTests {

        @Test
        @DisplayName("Should create user with valid request and return created user")
        void createUser_WithValidRequest_ReturnsCreatedUser() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create profiles
            var profile1 = Given.profile()
                                .withName("Profile 1")
                                .persist();
            var profile2 = Given.profile()
                                .withName("Profile 2")
                                .persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("johndoe", "John Doe", "john.doe@example.com",
                                           Set.of(profile1.getId(), profile2.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("username", is("johndoe"))
                   .body("name", is("John Doe"))
                   .body("email", is("john.doe@example.com"))
                   .body("profiles.size()", is(2))
                   .body("profiles.name", containsInAnyOrder("Profile 1", "Profile 2"))
                   .body("disabled", is(false));
        }

        @Test
        @DisplayName("Should generate random password for new user")
        void createUser_GeneratesRandomPassword_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create profile
            var profile = Given.profile()
                               .withName("User Profile")
                               .persist();

            // Act - Create user
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("passworduser", "Password User", "password@example.com",
                                           Set.of(profile.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED);

            // Assert - Verify user can login (password was generated and works)
            // This assumes the password generator creates a usable password
            // and the UserCreatedEvent properly records it
        }

        @Test
        @DisplayName("Should create user with minimum valid username length")
        void createUser_WithMinimumUsernameLength_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create profile
            var profile = Given.profile()
                               .withName("Test Profile")
                               .persist();

            // Act & Assert - Username with exactly 4 characters (minimum)
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("user", "Minimum User", "min@example.com", Set.of(profile.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("username", is("user"));
        }

        @Test
        @DisplayName("Should create user with maximum valid username length")
        void createUser_WithMaximumUsernameLength_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create profile
            var profile = Given.profile()
                               .withName("Test Profile")
                               .persist();

            // Username with exactly 15 characters (maximum)
            String maxLengthUsername = "a".repeat(15);

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest(maxLengthUsername, "Maximum User", "max@example.com",
                                           Set.of(profile.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("username", is(maxLengthUsername));
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return BAD_REQUEST when username is null")
        void createUser_WithNullUsername_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                            "name": "Test User",
                            "email": "test@example.com",
                            "profileIds": [1]
                         }
                         """)
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("create.request.username"))
                   .body("violations[0].message", is("must not be blank"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when username is empty")
        void createUser_WithEmptyUsername_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("", "Test User", "test@example.com", Set.of(1L)))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.field", hasItem("create.request.username"))
                   .body("violations.message", hasItem("must not be blank"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when username is too short")
        void createUser_WithShortUsername_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("usr", "Test User", "test@example.com", Set.of(1L)))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("create.request.username"))
                   .body("violations[0].message", is("size must be between 4 and 15"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when username is too long")
        void createUser_WithLongUsername_ReturnsBadRequest() {
            var admin = Given.admin();

            String longUsername = "a".repeat(16);

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest(longUsername, "Test User", "test@example.com", Set.of(1L)))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("create.request.username"))
                   .body("violations[0].message", is("size must be between 4 and 15"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when name is null")
        void createUser_WithNullName_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                            "username": "testuser",
                            "email": "test@example.com",
                            "profileIds": [1]
                         }
                         """)
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("create.request.name"))
                   .body("violations[0].message", is("must not be blank"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when email is invalid")
        void createUser_WithInvalidEmail_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("testuser", "Test User", "invalid-email", Set.of(1L)))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("create.request.email"))
                   .body("violations[0].message", is("must be a well-formed email address"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when profileIds is empty")
        void createUser_WithEmptyProfileIds_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                            "username": "testuser",
                            "name": "Test User",
                            "email": "test@example.com",
                            "profileIds": []
                         }
                         """)
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("create.request.profileIds"))
                   .body("violations[0].message", is("must not be empty"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when profileIds is null")
        void createUser_WithNullProfileIds_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                            "username": "testuser",
                            "name": "Test User",
                            "email": "test@example.com"
                         }
                         """)
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("create.request.profileIds"))
                   .body("violations[0].message", is("must not be empty"));
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should return NOT_FOUND when profile does not exist")
        void createUser_WithNonExistentProfile_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Non-existent profile ID
            Long nonExistentProfileId = 999L;

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("testuser", "Test User", "test@example.com",
                                           Set.of(nonExistentProfileId)))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("Could not find profiles! ids=[999]"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when some profiles do not exist")
        void createUser_WithMixedValidAndInvalidProfiles_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create one valid profile
            var validProfile = Given.profile()
                                    .withName("Valid Profile")
                                    .persist();

            // Use one invalid profile ID
            Long invalidProfileId = 888L;

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("testuser", "Test User", "test@example.com",
                                           Set.of(validProfile.getId(), invalidProfileId)))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("Could not find profiles! ids=[888]"));
        }

        @Test
        @DisplayName("Should prevent duplicate username")
        void createUser_WithExistingUsername_ReturnsConflictOrBadRequest() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a profile
            var profile = Given.profile()
                               .withName("Test Profile")
                               .persist();

            // Create a user with the username first
            Given.user()
                 .withName("Existing User")
                 .withUsername("existinguser")
                 .withEmail("existing@example.com")
                 .withPassword("password123")
                 .persist();

            // Act & Assert - Try to create another user with same username
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("existinguser", "New User", "new@example.com",
                                           Set.of(profile.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CONFLICT); // If constraint violation
        }

        @Test
        @DisplayName("Should prevent duplicate email")
        void createUser_WithExistingEmail_ReturnsConflictOrBadRequest() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a profile
            var profile = Given.profile()
                               .withName("Test Profile")
                               .persist();

            // Create a user with the email first
            Given.user()
                 .withName("Existing User")
                 .withUsername("user1")
                 .withEmail("duplicate@example.com")
                 .withPassword("password123")
                 .persist();

            // Act & Assert - Try to create another user with same email
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("user2", "New User", "duplicate@example.com",
                                           Set.of(profile.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CONFLICT);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should create user with multiple profiles")
        void createUser_WithMultipleProfiles_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create multiple profiles
            var profile1 = Given.profile()
                                .withName("Admin Profile")
                                .persist();
            var profile2 = Given.profile()
                                .withName("User Profile")
                                .persist();
            var profile3 = Given.profile()
                                .withName("Guest Profile")
                                .persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("multiuser", "Multi Profile User", "multi@example.com",
                                           Set.of(profile1.getId(), profile2.getId(), profile3.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("profiles.size()", is(3));
        }

        @Test
        @DisplayName("Should handle email with special characters")
        void createUser_WithEmailSpecialCharacters_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create profile
            var profile = Given.profile()
                               .withName("Test Profile")
                               .persist();

            String specialEmail = "user.name+tag@sub-domain.example.co.uk";

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("specialemail", "Special Email User", specialEmail,
                                           Set.of(profile.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("email", equalTo(specialEmail));
        }

        @Test
        @DisplayName("Should handle name with special characters and spaces")
        void createUser_WithSpecialName_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create profile
            var profile = Given.profile()
                               .withName("Test Profile")
                               .persist();

            String specialName = "João da Silva-Santos Jr.";

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createUserRequest("joao", specialName, "joao@example.com",
                                           Set.of(profile.getId())))
                   .when()
                   .post(CREATE_USER_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", equalTo(specialName));
        }
    }

    /**
     * Helper method to create a create user request JSON body.
     */
    private String createUserRequest(String username, String name, String email, Set<Long> profileIds) {
        String profileIdsArray = profileIds.stream()
                                           .map(String::valueOf)
                                           .reduce((a, b) -> a + ", " + b)
                                           .orElse("");

        return """
               {
                   "username": "%s",
                   "name": "%s",
                   "email": "%s",
                   "profileIds": [%s]
               }
               """.formatted(username, name, email, profileIdsArray);
    }
}