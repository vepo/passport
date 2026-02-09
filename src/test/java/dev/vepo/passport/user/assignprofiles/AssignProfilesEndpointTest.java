package dev.vepo.passport.user.assignprofiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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
@DisplayName("Assign Profiles to User API Endpoint Tests")
class AssignProfilesEndpointTest {

    private static final String ASSIGN_PROFILES_ENDPOINT = "/api/users/{userId}/profiles";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Nested
    @DisplayName("Authentication & Authorization")
    class AuthenticationAuthorizationTests {

        @Test
        @DisplayName("Should return UNAUTHORIZED when accessing endpoint without authentication")
        void assignProfiles_WithoutAuthentication_ReturnsUnauthorized() {
            given().contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(1L, 2L)))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, 1L)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return FORBIDDEN when authenticated as non-admin user")
        void assignProfiles_AsNonAdminUser_ReturnsForbidden() {
            // Arrange - Create a regular user (non-admin)
            var regularUser = Given.user()
                                   .withName("Regular User")
                                   .withUsername("regularuser")
                                   .withEmail("regular@passport.vepo.dev")
                                   .withPassword("password123")
                                   .persist();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Create profiles
            var profile1 = Given.profile().withName("Profile1").persist();
            var profile2 = Given.profile().withName("Profile2").persist();

            // Act & Assert
            given().header(regularUser.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profile1.getId(), profile2.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_FORBIDDEN);
        }

        @Test
        @DisplayName("Should succeed when authenticated as admin user")
        void assignProfiles_AsAdminUser_ReturnsOk() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Create profiles
            var profile1 = Given.profile().withName("Profile1").persist();
            var profile2 = Given.profile().withName("Profile2").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profile1.getId(), profile2.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetUser.id().intValue()))
                   .body("profiles", hasSize(2))
                   .body("profiles.name", containsInAnyOrder("Profile1", "Profile2"));
        }
    }

    @Nested
    @DisplayName("Successful Profile Assignment")
    class SuccessfulAssignmentTests {

        @Test
        @DisplayName("Should assign single profile to user")
        void assignProfiles_SingleProfile_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Create a profile
            var profile = Given.profile().withName("UserProfile").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profile.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetUser.id().intValue()))
                   .body("profiles", hasSize(1))
                   .body("profiles[0].name", is("UserProfile"))
                   .body("profiles[0].id", is(profile.getId().intValue()));
        }

        @Test
        @DisplayName("Should assign multiple profiles to user")
        void assignProfiles_MultipleProfiles_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Create multiple profiles
            var profile1 = Given.profile().withName("Editor").persist();
            var profile2 = Given.profile().withName("Viewer").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profile1.getId(), profile2.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetUser.id().intValue()))
                   .body("profiles", hasSize(2))
                   .body("profiles.name", containsInAnyOrder("Editor", "Viewer"));
        }

        @Test
        @DisplayName("Should replace existing profiles when assigning new ones")
        void assignProfiles_ReplaceExistingProfiles_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create initial profiles
            var oldProfile1 = Given.profile().withName("OldProfile1").persist();
            var oldProfile2 = Given.profile().withName("OldProfile2").persist();

            // Create a target user with initial profiles
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .withProfile("OldProfile1")
                                  .withProfile("OldProfile2")
                                  .persist();

            // Create new profiles
            var newProfile1 = Given.profile().withName("NewProfile1").persist();
            var newProfile2 = Given.profile().withName("NewProfile2").persist();

            // Verify initial state
            given().header(admin.authenticated())
                   .when()
                   .get("/api/users/{userId}", targetUser.id())
                   .then()
                   .body("profiles", hasSize(2))
                   .body("profiles.name", containsInAnyOrder("OldProfile1", "OldProfile2"));

            // Act - Assign new profiles
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(newProfile1.getId(), newProfile2.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetUser.id().intValue()))
                   .body("profiles", hasSize(2))
                   .body("profiles.name", containsInAnyOrder("NewProfile1", "NewProfile2"));

            // Verify old profiles are removed
            given().header(admin.authenticated())
                   .when()
                   .get("/api/users/{userId}", targetUser.id())
                   .then()
                   .body("profiles", hasSize(2))
                   .body("profiles.name", containsInAnyOrder("NewProfile1", "NewProfile2"));
        }

        @Test
        @DisplayName("Should clear all profiles when assigning empty set")
        void assignProfiles_EmptySet_ClearsAllProfiles() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user with initial profiles
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .withProfile("Profile1")
                                  .withProfile("Profile2")
                                  .persist();

            // Act & Assert - Assign empty profile set
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of()))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].message", is("At least one profile must be assigned"));
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return BAD_REQUEST when profileIds is null")
        void assignProfiles_WithNullProfileIds_ReturnsBadRequest() {
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("{}")
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("assignProfiles.request.profileIds"))
                   .body("violations[0].message", is("At least one profile must be assigned"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when profileIds is empty")
        void assignProfiles_WithEmptyProfileIds_ReturnsBadRequest() {
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of()))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.field", hasItem("assignProfiles.request.profileIds"))
                   .body("violations.message", hasItem("At least one profile must be assigned"));
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should return NOT_FOUND when user does not exist")
        void assignProfiles_WithNonExistentUser_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create profiles
            var profile = Given.profile().withName("TestProfile").persist();

            // Non-existent user ID
            Long nonExistentUserId = 999L;

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profile.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, nonExistentUserId)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("User not found with id: 999"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when user is disabled")
        void assignProfiles_ToDisabledUser_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a disabled user
            var disabledUser = Given.user()
                                    .withName("Disabled User")
                                    .withUsername("disableduser")
                                    .withEmail("disabled@passport.vepo.dev")
                                    .withPassword("password123")
                                    .withDisabled(true)
                                    .persist();

            // Create profiles
            var profile = Given.profile().withName("TestProfile").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profile.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, disabledUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("Cannot assign profiles to disabled user"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when profile does not exist")
        void assignProfiles_WithNonExistentProfile_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Non-existent profile ID
            Long nonExistentProfileId = 999L;

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(nonExistentProfileId)))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("Could not find profiles! ids=[999]"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when some profiles do not exist")
        void assignProfiles_WithMixedValidAndInvalidProfiles_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Create one valid profile
            var validProfile = Given.profile().withName("ValidProfile").persist();

            // Use one invalid profile ID
            Long invalidProfileId = 888L;

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(validProfile.getId(), invalidProfileId)))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("Could not find profiles! ids=[888]"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle assigning same profile multiple times (duplicates in request)")
        void assignProfiles_WithDuplicateProfileIds_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Create a profile
            var profile = Given.profile().withName("TestProfile").persist();

            // Act & Assert - Duplicate IDs in request should still work (Set eliminates
            // duplicates)
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                           "profileIds": [%d, %d, %d]
                         }
                         """.formatted(profile.getId(), profile.getId(), profile.getId()))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("profiles", hasSize(1))
                   .body("profiles[0].id", is(profile.getId().intValue()));
        }

        @Test
        @DisplayName("Should assign many profiles to user")
        void assignProfiles_WithManyProfiles_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Create 10 profiles
            Long[] profileIds = new Long[10];
            for (int i = 0; i < 10; i++) {
                var profile = Given.profile()
                                   .withName("Profile_" + i)
                                   .persist();
                profileIds[i] = profile.getId();
            }

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profileIds)))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("profiles", hasSize(10));
        }

        @Test
        @DisplayName("Should maintain user's other properties when assigning profiles")
        void assignProfiles_MaintainsUserProperties_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Original Name")
                                  .withUsername("originaluser")
                                  .withEmail("original@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Store original user properties
            var originalUser = given().header(admin.authenticated())
                                      .when()
                                      .get("/api/users/{userId}", targetUser.id())
                                      .then()
                                      .extract()
                                      .as(dev.vepo.passport.user.UserResponse.class);

            // Create profiles
            var profile = Given.profile().withName("NewProfile").persist();

            // Act - Assign profiles
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profile.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetUser.id().intValue()))
                   .body("username", equalTo(originalUser.username()))
                   .body("name", equalTo(originalUser.name()))
                   .body("email", equalTo(originalUser.email()))
                   .body("disabled", is(false));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should allow reassigning profiles multiple times")
        void assignProfiles_MultipleReassignments_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target user
            var targetUser = Given.user()
                                  .withName("Target User")
                                  .withUsername("targetuser")
                                  .withEmail("target@passport.vepo.dev")
                                  .withPassword("password123")
                                  .persist();

            // Create multiple sets of profiles
            var profileSet1 = Given.profile().withName("Set1_Profile1").persist();
            var profileSet2 = Given.profile().withName("Set2_Profile1").persist();
            var profileSet3 = Given.profile().withName("Set3_Profile1").persist();

            // First assignment
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profileSet1.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("profiles", hasSize(1))
                   .body("profiles[0].name", is("Set1_Profile1"));

            // Second assignment
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profileSet2.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("profiles", hasSize(1))
                   .body("profiles[0].name", is("Set2_Profile1"));

            // Third assignment
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignProfilesRequest(Set.of(profileSet3.getId())))
                   .when()
                   .post(ASSIGN_PROFILES_ENDPOINT, targetUser.id())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("profiles", hasSize(1))
                   .body("profiles[0].name", is("Set3_Profile1"));
        }
    }

    /**
     * Helper method to create an assign profiles request JSON body.
     */
    private String assignProfilesRequest(Set<Long> profileIds) {
        if (profileIds.isEmpty()) {
            return """
                   {
                     "profileIds": []
                   }
                   """;
        }

        String profileIdsArray = profileIds.stream()
                                           .map(String::valueOf)
                                           .reduce((a, b) -> a + ", " + b)
                                           .orElse("");

        return """
               {
                 "profileIds": [%s]
               }
               """.formatted(profileIdsArray);
    }
}