package dev.vepo.passport.user.update;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Set;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.passport.model.Profile;
import dev.vepo.passport.shared.Given;
import dev.vepo.passport.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
@DisplayName("Update User Endpoint")
class UpdateUserEndpointTest {

    private static final String UPDATE_USER_PATH = "/api/users/:id";

    @Inject
    UserRepository userRepository;

    @Inject
    ObjectMapper objectMapper;

    private Given.GivenUser admin;
    private Given.GivenUser regularUser;
    private Given.GivenUser otherUser;
    private Profile userProfile;
    private Profile editorProfile;
    private Profile adminProfile;

    @BeforeEach
    void setUp() {
        Given.cleanup();

        // Create profiles
        userProfile = Given.profile()
                           .withName("USER")
                           .persist();

        editorProfile = Given.profile()
                             .withName("EDITOR")
                             .persist();

        adminProfile = Given.profile()
                            .withName("ADMIN")
                            .persist();

        // Create users
        admin = Given.admin(); // This already has ADMIN profile

        regularUser = Given.user()
                           .withUsername("testuser")
                           .withEmail("test@example.com")
                           .withName("Test User")
                           .withPassword("12345")
                           .withProfile("USER")
                           .persist();

        otherUser = Given.user()
                         .withUsername("otheruser")
                         .withEmail("other@example.com")
                         .withName("Other User")
                         .withPassword("12345")
                         .withProfile("USER")
                         .persist();
    }

    @Test
    @DisplayName("PUT /users/{userId} - should update user successfully")
    void updateUser_ValidRequest_ShouldUpdateSuccessfully() {
        // Given
        long userId = regularUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Name",
                                                  "updated@example.com",
                                                  Set.of(userProfile.getId(), editorProfile.getId()));

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("username", is(regularUser.username())) // Username shouldn't change
               .body("name", is("Updated Name"))
               .body("email", is("updated@example.com"))
               .body("disabled", is(false))
               .body("profiles.name", hasItems("USER", "EDITOR"));

        // Verify database state
        var updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(updatedUser.getProfiles()).hasSize(2);
        assertThat(updatedUser.getProfiles().stream().map(Profile::getName))
                                                                            .containsExactlyInAnyOrder("USER", "EDITOR");
    }

    @Test
    @DisplayName("PUT /users/{userId} - should update only name and email when no profiles specified")
    void updateUser_NoProfilesSpecified_ShouldUpdateWithoutChangingProfiles() {
        // Given - user with existing profiles
        var userWithProfiles = Given.user()
                                    .withUsername("withprofiles")
                                    .withEmail("withprofiles@example.com")
                                    .withName("With Profiles")
                                    .withPassword("12345")
                                    .withProfile("USER")
                                    .withProfile("EDITOR")
                                    .persist();

        long userId = userWithProfiles.id();
        var updateRequest = new UpdateUserRequest(
                                                  "New Name",
                                                  "newemail@example.com",
                                                  null // Profiles not specified
        );

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("name", is("New Name"))
               .body("email", is("newemail@example.com"))
               .body("profiles.size()", is(2)); // Should keep existing profiles

        // Verify database state
        var updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getName()).isEqualTo("New Name");
        assertThat(updatedUser.getEmail()).isEqualTo("newemail@example.com");
        assertThat(updatedUser.getProfiles()).hasSize(2);
    }

    @Test
    @DisplayName("PUT /users/{userId} - should clear profiles when empty set provided")
    void updateUser_EmptyProfileSet_ShouldClearProfiles() {
        // Given - user with existing profiles
        var userWithProfiles = Given.user()
                                    .withUsername("clearprofiles")
                                    .withEmail("clearprofiles@example.com")
                                    .withName("Clear Profiles")
                                    .withPassword("12345")
                                    .withProfile("USER")
                                    .persist();

        long userId = userWithProfiles.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Name",
                                                  "updated@example.com",
                                                  Set.of() // Empty set
        );

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("profiles", empty());

        // Verify database state
        var updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getProfiles()).isEmpty();
    }

    @Test
    @DisplayName("PUT /users/{userId} - should return 409 when email already exists")
    void updateUser_DuplicateEmail_ShouldReturnConflict() {
        // Given
        long userId = regularUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Name",
                                                  otherUser.email(), // Using other user's email
                                                  Set.of(userProfile.getId()));

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_CONFLICT)
               .body("message", is("Email 'other@example.com' is already registered"));

        // Verify original user not changed
        var originalUser = userRepository.findById(userId).orElseThrow();
        assertThat(originalUser.getEmail()).isEqualTo(regularUser.email());
        assertThat(originalUser.getName()).isEqualTo(regularUser.name());
    }

    @Test
    @DisplayName("PUT /users/{userId} - should allow same email when not changed")
    void updateUser_SameEmail_ShouldUpdateSuccessfully() {
        // Given
        long userId = regularUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Name",
                                                  regularUser.email(), // Same email as before
                                                  Set.of(userProfile.getId()));

        // When & Then
        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("email", is(regularUser.email()))
               .body("name", is("Updated Name"));

        // Verify database state
        var updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getEmail()).isEqualTo(regularUser.email());
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("PUT /users/{userId} - should return 404 for non-existent user")
    void updateUser_NonExistentUser_ShouldReturnNotFound() {
        long nonExistentId = 999999L;
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Name",
                                                  "updated@example.com",
                                                  Set.of(userProfile.getId()));

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(nonExistentId)))
               .then()
               .statusCode(HttpStatus.SC_NOT_FOUND)
               .body("message", is("User not found! userId=999999"));
    }

    @Test
    @DisplayName("PUT /users/{userId} - should return 403 for non-admin user")
    void updateUser_NonAdminUser_ShouldReturnForbidden() {
        long userId = otherUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Name",
                                                  "updated@example.com",
                                                  Set.of(userProfile.getId()));

        given().header(regularUser.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("PUT /users/{userId} - should return 401 for unauthenticated request")
    void updateUser_Unauthenticated_ShouldReturnUnauthorized() {
        long userId = regularUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Name",
                                                  "updated@example.com",
                                                  Set.of(userProfile.getId()));

        given().contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("PUT /users/{userId} - admin should be able to update themselves")
    void updateUser_AdminUpdatingSelf_ShouldUpdateSuccessfully() {
        long adminId = admin.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Admin Name",
                                                  "admin.updated@example.com",
                                                  Set.of(adminProfile.getId()));

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(adminId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) adminId))
               .body("name", is("Updated Admin Name"))
               .body("email", is("admin.updated@example.com"));

        // Verify database state
        var updatedAdmin = userRepository.findById(adminId).orElseThrow();
        assertThat(updatedAdmin.getName()).isEqualTo("Updated Admin Name");
        assertThat(updatedAdmin.getEmail()).isEqualTo("admin.updated@example.com");
    }

    @Test
    @DisplayName("PUT /users/{userId} - should return 400 for invalid profile IDs")
    void updateUser_InvalidProfileIds_ShouldReturnBadRequest() {
        long userId = regularUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Updated Name",
                                                  "updated@example.com",
                                                  Set.of(999999L) // Non-existent profile ID
        );

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_BAD_REQUEST)
               .body(containsString("One or more profiles not found"));
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for empty name")
        void updateUser_EmptyName_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String invalidRequest = """
                                    {
                                        "name": "",
                                        "email": "updated@example.com",
                                        "profileIds": []
                                    }
                                    """;

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for invalid email")
        void updateUser_InvalidEmail_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String invalidRequest = """
                                    {
                                        "name": "Updated Name",
                                        "email": "invalid-email",
                                        "profileIds": []
                                    }
                                    """;

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'update.request.email' }.message",
                         containsString("must be a well-formed email address"));
        }

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for null name")
        void updateUser_NullName_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String invalidRequest = """
                                    {
                                        "email": "updated@example.com",
                                        "profileIds": []
                                    }
                                    """;

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'update.request.name' }.message",
                         is("must not be blank"));
        }

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for null email")
        void updateUser_NullEmail_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String invalidRequest = """
                                    {
                                        "name": "Updated Name",
                                        "profileIds": []
                                    }
                                    """;

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'update.request.email' }.message",
                         is("must not be blank"));
        }

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for very long name")
        void updateUser_LongName_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String longName = "A".repeat(256); // Assuming @Size(max = 255)
            String invalidRequest = String.format("""
                                                  {
                                                      "name": "%s",
                                                      "email": "updated@example.com",
                                                      "profileIds": []
                                                  }
                                                  """, longName);

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'update.request.name' }.message",
                         containsString("size must be between"));
        }

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for very long email")
        void updateUser_LongEmail_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String longEmail = "a".repeat(250) + "@example.com"; // > 255 chars
            String invalidRequest = String.format("""
                                                  {
                                                      "name": "Updated Name",
                                                      "email": "%s",
                                                      "profileIds": []
                                                  }
                                                  """, longEmail);

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for multiple violations")
        void updateUser_MultipleViolations_ShouldReturnAllErrors() {
            long userId = regularUser.id();
            String invalidRequest = """
                                    {
                                        "name": "",
                                        "email": "invalid-email",
                                        "profileIds": []
                                    }
                                    """;

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.size()", is(3));
        }

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for name with only whitespace")
        void updateUser_WhitespaceName_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String invalidRequest = """
                                    {
                                        "name": "   ",
                                        "email": "updated@example.com",
                                        "profileIds": []
                                    }
                                    """;

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'update.request.name' }.message",
                         is("must not be blank"));
        }

        @Test
        @DisplayName("PUT /users/{userId} - should return 400 for email with only whitespace")
        void updateUser_WhitespaceEmail_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String invalidRequest = """
                                    {
                                        "name": "Updated Name",
                                        "email": "   ",
                                        "profileIds": []
                                    }
                                    """;

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("PUT /users/{userId} - should validate profileIds format")
        void updateUser_InvalidProfileIdsFormat_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String invalidRequest = """
                                    {
                                        "name": "Updated Name",
                                        "email": "updated@example.com",
                                        "profileIds": ["invalid", "format"]
                                    }
                                    """;

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("PUT /users/{userId} - should handle empty JSON")
        void updateUser_EmptyJson_ShouldReturnBadRequest() {
            long userId = regularUser.id();
            String invalidRequest = "{}";

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(invalidRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.size()", is(2))
                   .body("violations.field", hasItems("update.request.name", "update.request.email"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("PUT /users/{userId} - should handle case-insensitive email conflict check")
        void updateUser_CaseInsensitiveEmail_ShouldReturnConflict() {
            // Given - user with email "test@example.com"
            long userId = regularUser.id();

            // Try to update to same email but different case
            var updateRequest = new UpdateUserRequest(
                                                      "Updated Name",
                                                      "TEST@EXAMPLE.COM", // Same email, different case
                                                      Set.of(userProfile.getId()));

            // When & Then - Should allow because it's the same user
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(updateRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("email", is("TEST@EXAMPLE.COM"));
        }

        @Test
        @DisplayName("PUT /users/{userId} - should update disabled user")
        void updateUser_DisabledUser_ShouldUpdateSuccessfully() {
            // Given - create a disabled user
            var disabledUser = Given.user()
                                    .withUsername("disableduser")
                                    .withEmail("disabled@example.com")
                                    .withName("Disabled User")
                                    .withPassword("12345")
                                    .withDisabled(true)
                                    .persist();

            long userId = disabledUser.id();
            var updateRequest = new UpdateUserRequest(
                                                      "Enabled User Name",
                                                      "enabled@example.com",
                                                      Set.of(userProfile.getId()));

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(updateRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("disabled", is(true)) // Should still be disabled
                   .body("name", is("Enabled User Name"))
                   .body("email", is("enabled@example.com"));
        }

        @Test
        @DisplayName("PUT /users/{userId} - should preserve username even when updating other fields")
        void updateUser_ShouldNotChangeUsername() {
            long userId = regularUser.id();
            var updateRequest = new UpdateUserRequest(
                                                      "Completely New Name",
                                                      "completely.new@example.com",
                                                      Set.of(editorProfile.getId()));

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(updateRequest)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("username", is(regularUser.username())) // Username unchanged
                   .body("name", not(is(regularUser.name()))) // Name changed
                   .body("email", not(is(regularUser.email()))); // Email changed
        }

        @Test
        @DisplayName("PUT /users/{userId} - should handle multiple profile updates")
        void updateUser_MultipleProfileChanges_ShouldUpdateCorrectly() {
            long userId = regularUser.id();

            // First update: Add EDITOR profile
            var firstUpdate = new UpdateUserRequest(
                                                    "First Update",
                                                    "first@example.com",
                                                    Set.of(userProfile.getId(), editorProfile.getId()));

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(firstUpdate)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("profiles.size()", is(2));

            // Second update: Remove EDITOR profile, keep only USER
            var secondUpdate = new UpdateUserRequest(
                                                     "Second Update",
                                                     "second@example.com",
                                                     Set.of(userProfile.getId()));

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(secondUpdate)
                   .when()
                   .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("profiles.size()", is(1))
                   .body("profiles[0].name", is("USER"));

            // Verify final state
            var finalUser = userRepository.findById(userId).orElseThrow();
            assertThat(finalUser.getProfiles()).hasSize(1);
            assertThat(finalUser.getProfiles().iterator().next().getName()).isEqualTo("USER");
            assertThat(finalUser.getName()).isEqualTo("Second Update");
            assertThat(finalUser.getEmail()).isEqualTo("second@example.com");
        }
    }

    @Test
    @DisplayName("PUT /users/{userId} - should return complete response structure")
    void updateUser_ShouldReturnCompleteResponse() {
        long userId = regularUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Complete Response Test",
                                                  "complete@example.com",
                                                  Set.of(userProfile.getId(), editorProfile.getId()));

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("id", is((int) userId))
               .body("username", notNullValue())
               .body("email", notNullValue())
               .body("name", notNullValue())
               .body("disabled", notNullValue())
               .body("createdAt", notNullValue())
               .body("updatedAt", notNullValue())
               .body("profiles", notNullValue());
    }

    @Test
    @DisplayName("PUT /users/{userId} - should update user with special characters")
    void updateUser_SpecialCharacters_ShouldUpdateSuccessfully() {
        long userId = regularUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Special Name O'Brian-Smith",
                                                  "special.name@example-domain.com",
                                                  Set.of(userProfile.getId()));

        given().header(admin.authenticated())
               .contentType(ContentType.JSON)
               .body(updateRequest)
               .when()
               .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("name", is("Special Name O'Brian-Smith"))
               .body("email", is("special.name@example-domain.com"));
    }

    @Test
    @DisplayName("PUT /users/{userId} - should not expose sensitive information")
    void updateUser_ShouldNotExposeSensitiveInformation() {
        long userId = regularUser.id();
        var updateRequest = new UpdateUserRequest(
                                                  "Test Name",
                                                  "test@example.com",
                                                  Set.of(userProfile.getId()));

        String response = given().header(admin.authenticated())
                                 .contentType(ContentType.JSON)
                                 .body(updateRequest)
                                 .when()
                                 .put(UPDATE_USER_PATH.replace(":id", Long.toString(userId)))
                                 .then()
                                 .statusCode(HttpStatus.SC_OK)
                                 .extract()
                                 .asString();

        assertThat(response)
                            .doesNotContain("password")
                            .doesNotContain("encodedPassword")
                            .doesNotContain("12345"); // Original password
    }
}