package dev.vepo.passport.user.search;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.model.Profile;
import dev.vepo.passport.model.Role;
import dev.vepo.passport.shared.Given;
import dev.vepo.passport.user.UserRepository;
import dev.vepo.passport.user.UserResponse;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import jakarta.inject.Inject;

@QuarkusTest
@DisplayName("Search User Endpoint")
class SearchUserEndpointTest {

    private static final String SEARCH_USERS_PATH = "/api/users/search";

    @Inject
    UserRepository userRepository;

    private Given.GivenUser admin;
    private Given.GivenUser user1;
    private Given.GivenUser user2;
    private Given.GivenUser user3;
    private Given.GivenUser disabledUser;
    private Profile userProfile;
    private Profile editorProfile;
    private Role userRole;

    @BeforeEach
    void setUp() {
        Given.cleanup();

        // Create roles
        Given.role()
             .withName("ADMIN")
             .persist();
        userRole = Given.role()
                        .withName("USER")
                        .persist();

        // Create profiles with roles
        Given.profile()
             .withName("Administrator")
             .withRole("ADMIN")
             .persist();

        userProfile = Given.profile()
                           .withName("Regular User")
                           .withRole("USER")
                           .persist();

        editorProfile = Given.profile()
                             .withName("Editor")
                             .withRole("USER")
                             .persist();

        // Create users
        admin = Given.admin(); // This should have ADMIN profile

        user1 = Given.user()
                     .withUsername("john.doe")
                     .withEmail("john.doe@example.com")
                     .withName("John Doe")
                     .withPassword("Password123")
                     .withProfile("Regular User")
                     .persist();

        user2 = Given.user()
                     .withUsername("jane.smith")
                     .withEmail("jane.smith@example.com")
                     .withName("Jane Smith")
                     .withPassword("Password123")
                     .withProfile("Editor")
                     .persist();

        user3 = Given.user()
                     .withUsername("bob.johnson")
                     .withEmail("bob.j@example.com")
                     .withName("Bob Johnson")
                     .withPassword("Password123")
                     .withProfile("Regular User")
                     .withProfile("Editor")
                     .persist();

        disabledUser = Given.user()
                            .withUsername("inactive.user")
                            .withEmail("inactive@example.com")
                            .withName("Inactive User")
                            .withPassword("Password123")
                            .withDisabled(true)
                            .withProfile("Regular User")
                            .persist();
    }

    @Test
    @DisplayName("GET /users/search - should return all active users when admin with no filters")
    void searchUsers_NoFilters_ShouldReturnAllActiveUsers() {
        given().header(admin.authenticated())
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(4)) // admin, user1, user2, user3 (disabledUser excluded)
               .body("username", hasItems(admin.username(),
                                          user1.username(),
                                          user2.username(),
                                          user3.username()))
               .body("username", not(hasItem(disabledUser.username()))) // Should not include disabled users
               .body("findAll { it.disabled == true }", empty()); // No disabled users should be returned
    }

    @Test
    @DisplayName("GET /users/search - should search by name")
    void searchUsers_ByName_ShouldReturnMatchingUsers() {
        given().header(admin.authenticated())
               .queryParam("name", "John")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(2)) // John Doe and Bob Johnson
               .body("username", hasItems(user1.username(), user3.username()))
               .body("username", not(hasItem(user2.username())));
    }

    @Test
    @DisplayName("GET /users/search - should search by email")
    void searchUsers_ByEmail_ShouldReturnMatchingUsers() {
        given().header(admin.authenticated())
               .queryParam("email", "example.com")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(3)) // All active users have example.com email
               .body("email", everyItem(containsString("example.com")));
    }

    @Test
    @DisplayName("GET /users/search - should search by specific email")
    void searchUsers_BySpecificEmail_ShouldReturnSingleUser() {
        given().header(admin.authenticated())
               .queryParam("email", "jane.smith@example.com")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(1))
               .body("[0].username", is(user2.username()))
               .body("[0].email", is(user2.email()));
    }

    @Test
    @DisplayName("GET /users/search - should search by profile IDs")
    void searchUsers_ByProfileIds_ShouldReturnUsersWithProfiles() {
        Long userProfileId = userProfile.getId();

        given().header(admin.authenticated())
               .queryParam("profiles", userProfileId)
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(2)) // user1, user3, admin (has ADMIN profile which might include USER role)
               .body("username", hasItems(user1.username(), user3.username()));
    }

    @Test
    @DisplayName("GET /users/search - should search by multiple profile IDs")
    void searchUsers_ByMultipleProfileIds_ShouldReturnUsersWithAnyProfile() {
        Long userProfileId = userProfile.getId();
        Long editorProfileId = editorProfile.getId();

        given().header(admin.authenticated())
               .queryParam("profiles", userProfileId, editorProfileId)
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(3)) // user1 (user), user2 (editor), user3 (both)
               .body("username", hasItems(user1.username(), user2.username(), user3.username()));
    }

    @Test
    @DisplayName("GET /users/search - should search by role IDs")
    void searchUsers_ByRoleIds_ShouldReturnUsersWithRoles() {
        Long userRoleId = userRole.getId();

        given().header(admin.authenticated())
               .queryParam("roles", userRoleId)
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(3)) // user1, user2, user3 (all have USER role)
               .body("username", hasItems(user1.username(), user2.username(), user3.username()));
    }

    @Test
    @DisplayName("GET /users/search - should combine multiple search criteria")
    void searchUsers_WithMultipleCriteria_ShouldReturnIntersection() {
        given().header(admin.authenticated())
               .queryParam("name", "John")
               .queryParam("profiles", userProfile.getId())
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(2)) // John Doe and Bob Johnson both have "John" in name and USER profile
               .body("username", hasItems(user1.username(), user3.username()));
    }

    @Test
    @DisplayName("GET /users/search - should return empty list for non-matching criteria")
    void searchUsers_NonMatchingCriteria_ShouldReturnEmptyList() {
        given().header(admin.authenticated())
               .queryParam("name", "Nonexistent User")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", empty());
    }

    @Test
    @DisplayName("GET /users/search - should be case insensitive for name and email")
    void searchUsers_CaseInsensitive_ShouldReturnResults() {
        given().header(admin.authenticated())
               .queryParam("name", "JOHN")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(2)) // Should still find "John Doe" and "Bob Johnson"
               .body("username", hasItems(user1.username(), user3.username()));
    }

    @Test
    @DisplayName("GET /users/search - should return 403 for non-admin user")
    void searchUsers_NonAdminUser_ShouldReturnForbidden() {
        given().header(user1.authenticated())
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("GET /users/search - should return 401 for unauthenticated request")
    void searchUsers_Unauthenticated_ShouldReturnUnauthorized() {
        given().when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /users/search - should handle empty query parameters gracefully")
    void searchUsers_EmptyQueryParams_ShouldReturnAllActiveUsers() {
        given().header(admin.authenticated())
               .queryParam("name", "")
               .queryParam("email", "")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(4)) // All active users
               .body("findAll { it.disabled == true }", empty());
    }

    @Test
    @DisplayName("GET /users/search - should support partial name matching")
    void searchUsers_PartialName_ShouldReturnMatchingUsers() {
        given().header(admin.authenticated())
               .queryParam("name", "Do")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(1))
               .body("[0].username", is(user1.username()));
    }

    @Test
    @DisplayName("GET /users/search - should support partial email matching")
    void searchUsers_PartialEmail_ShouldReturnMatchingUsers() {
        given().header(admin.authenticated())
               .queryParam("email", "smith")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(1))
               .body("[0].username", is(user2.username()));
    }

    @Test
    @DisplayName("GET /users/search - should return users ordered by name")
    void searchUsers_ShouldReturnUsersOrderedByName() {
        List<String> userNames = given().header(admin.authenticated())
                                        .when()
                                        .get(SEARCH_USERS_PATH)
                                        .then()
                                        .statusCode(HttpStatus.SC_OK)
                                        .extract()
                                        .jsonPath()
                                        .getList("name");

        // Verify names are in alphabetical order
        List<String> sortedNames = userNames.stream().sorted().collect(Collectors.toList());
        assertThat(userNames).isEqualTo(sortedNames);
    }

    @Test
    @DisplayName("GET /users/search - should exclude disabled users from all searches")
    void searchUsers_ShouldAlwaysExcludeDisabledUsers() {
        // Search with criteria that would match disabled user
        given().header(admin.authenticated())
               .queryParam("name", "Inactive")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", empty());

        given().header(admin.authenticated())
               .queryParam("email", "inactive@example.com")
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", empty());

        given().header(admin.authenticated())
               .queryParam("profiles", userProfile.getId())
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .body("username", not(hasItem(disabledUser.username())));
    }

    @Test
    @DisplayName("GET /users/search - should return complete user response structure")
    void searchUsers_ShouldReturnCompleteResponseStructure() {
        given().header(admin.authenticated())
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("[0]", hasKey("id"))
               .body("[0]", hasKey("username"))
               .body("[0]", hasKey("email"))
               .body("[0]", hasKey("name"))
               .body("[0]", hasKey("disabled"))
               .body("[0]", hasKey("createdAt"))
               .body("[0]", hasKey("updatedAt"))
               .body("[0]", hasKey("profiles"));
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("GET /users/search - should handle special characters in search terms")
        void searchUsers_SpecialCharacters_ShouldHandleProperly() {
            var specialUser = Given.user()
                                   .withUsername("special-user")
                                   .withEmail("special.user@example.com")
                                   .withName("Special-User O'Brian")
                                   .withPassword("Password123")
                                   .persist();

            given().header(admin.authenticated())
                   .queryParam("name", "O'Brian")
                   .when()
                   .get(SEARCH_USERS_PATH)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(1))
                   .body("[0].username", is(specialUser.username()));
        }

        @Test
        @DisplayName("GET /users/search - should handle very long search terms")
        void searchUsers_LongSearchTerm_ShouldHandleProperly() {
            String longName = "A".repeat(100);
            Given.user()
                 .withUsername("longname")
                 .withEmail("long@example.com")
                 .withName(longName)
                 .withPassword("Password123")
                 .persist();

            given().header(admin.authenticated())
                   .queryParam("name", "A")
                   .when()
                   .get(SEARCH_USERS_PATH)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("find { it.username == 'longname' }.name", is(longName));
        }

        @Test
        @DisplayName("GET /users/search - should handle null/empty profile and role lists")
        void searchUsers_NullEmptyLists_ShouldHandleProperly() {
            // Test with empty lists (should ignore them)
            given().header(admin.authenticated())
                   .queryParam("profiles", "")
                   .queryParam("roles", "")
                   .when()
                   .get(SEARCH_USERS_PATH)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(4)); // All active users
        }
    }

    @Test
    @DisplayName("GET /users/search - should paginate results if needed")
    void searchUsers_ShouldReturnReasonableNumberOfResults() {
        // Create many users to test that search doesn't return unreasonable number
        for (int i = 0; i < 10; i++) {
            Given.user()
                 .withUsername("bulkuser" + i)
                 .withEmail("bulk" + i + "@example.com")
                 .withName("Bulk User " + i)
                 .withPassword("Password123")
                 .persist();
        }

        given().header(admin.authenticated())
               .when()
               .get(SEARCH_USERS_PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(14)); // original 4 + 10 new users
    }

    @Test
    @DisplayName("GET /users/search - should validate response types")
    void searchUsers_ShouldReturnCorrectDataTypes() {
        List<UserResponse> users = given().header(admin.authenticated())
                                          .when()
                                          .get(SEARCH_USERS_PATH)
                                          .then()
                                          .statusCode(HttpStatus.SC_OK)
                                          .extract()
                                          .as(new TypeRef<List<UserResponse>>() {});

        // Verify all fields have correct types
        for (UserResponse user : users) {
            assertThat(user.id()).isInstanceOf(Long.class);
            assertThat(user.username()).isInstanceOf(String.class);
            assertThat(user.email()).isInstanceOf(String.class);
            assertThat(user.name()).isInstanceOf(String.class);
            assertThat(user.disabled()).isInstanceOf(Boolean.class);
            assertThat(user.createdAt()).isNotNull();
            assertThat(user.updatedAt()).isNotNull();
            assertThat(user.profiles()).isNotNull();
        }
    }
}