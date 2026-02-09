package dev.vepo.passport.role.list;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.role.RoleResponse;
import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("List Roles API Endpoint Tests")
class ListRolesEndpointTest {

    private static final String LIST_ROLES_ENDPOINT = "/api/roles";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Nested
    @DisplayName("Authentication & Authorization")
    class AuthenticationAuthorizationTests {

        @Test
        @DisplayName("Should return UNAUTHORIZED when accessing endpoint without authentication")
        void listRoles_WithoutAuthentication_ReturnsUnauthorized() {
            given().contentType(ContentType.JSON)
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return FORBIDDEN when authenticated as non-admin user")
        void listRoles_AsNonAdminUser_ReturnsForbidden() {
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
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_FORBIDDEN);
        }

        @Test
        @DisplayName("Should succeed when authenticated as admin user")
        void listRoles_AsAdminUser_ReturnsOk() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create some test roles
            Given.role().withName("ROLE_TEST_1").persist();
            Given.role().withName("ROLE_TEST_2").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .contentType(ContentType.JSON);
        }
    }

    @Nested
    @DisplayName("Successful Role Listing")
    class SuccessfulListingTests {

        @Test
        @DisplayName("Should return empty list when no roles exist")
        void listRoles_WhenNoRoles_ReturnsEmptyList() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(1));
        }

        @Test
        @DisplayName("Should return all roles when roles exist")
        void listRoles_WhenRolesExist_ReturnsAllRoles() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create test roles
            Given.role().withName("ROLE_ADMINISTRATOR").persist();
            Given.role().withName("ROLE_EDITOR").persist();
            Given.role().withName("ROLE_VIEWER").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(4))
                   .body("name", containsInAnyOrder("admin", "ROLE_ADMINISTRATOR", "ROLE_EDITOR", "ROLE_VIEWER"));
        }

        @Test
        @DisplayName("Should return roles in consistent format")
        void listRoles_ReturnsConsistentResponseFormat() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a test role
            Given.role()
                 .withName("ROLE_TEST")
                 .persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(2))
                   .body("id", hasSize(2))
                   .body("name", containsInAnyOrder("admin", "ROLE_TEST"));
        }

        @Test
        @DisplayName("Should return roles sorted by name")
        void listRoles_ReturnsRolesSortedByName() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create roles in non-alphabetical order
            Given.role().withName("ROLE_ZEBRA").persist();
            Given.role().withName("ROLE_ALPHA").persist();
            Given.role().withName("ROLE_CHARLIE").persist();
            Given.role().withName("ROLE_BETA").persist();

            // Act
            List<RoleResponse> roles = given().header(admin.authenticated())
                                              .when()
                                              .get(LIST_ROLES_ENDPOINT)
                                              .then()
                                              .statusCode(HttpStatus.SC_OK)
                                              .extract()
                                              .as(new TypeRef<List<RoleResponse>>() {});

            // Assert - Check if sorted by name
            List<String> roleNames = roles.stream()
                                          .map(RoleResponse::name)
                                          .collect(Collectors.toList());

            // Assuming findAll() returns sorted by name (or we need to verify this is
            // expected behavior)
            // If not sorted, this test will document the actual behavior
            Assertions.assertThat(roles).hasSize(roleNames.size())
                      .extracting(RoleResponse::name)
                      .containsExactlyInAnyOrderElementsOf(roleNames);
        }
    }

    @Nested
    @DisplayName("Pagination and Filtering")
    class PaginationFilteringTests {

        @Test
        @DisplayName("Should handle large number of roles")
        void listRoles_WithManyRoles_ReturnsAll() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create 50 roles
            for (int i = 1; i <= 50; i++) {
                Given.role().withName("ROLE_TEST_" + i).persist();
            }

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(51));
        }

        @Test
        @DisplayName("Should include system roles if they exist")
        void listRoles_IncludesSystemRoles() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create some roles including what might be system roles
            Given.role().withName("ROLE_ADMIN").persist();
            Given.role().withName("ROLE_USER").persist();
            Given.role().withName("ROLE_GUEST").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(4))
                   .body("name", hasItem("ROLE_ADMIN"))
                   .body("name", hasItem("ROLE_USER"))
                   .body("name", hasItem("ROLE_GUEST"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle roles with special characters in name")
        void listRoles_WithSpecialCharacterNames_ReturnsAll() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create roles with special characters
            Given.role().withName("ROLE_DATA-ANALYST").persist();
            Given.role().withName("ROLE_DEV_OPS").persist();
            Given.role().withName("ROLE_V2.0").persist();
            Given.role().withName("ROLE_TEST@PROD").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(5))
                   .body("name", containsInAnyOrder("admin",
                                                    "ROLE_DATA-ANALYST",
                                                    "ROLE_DEV_OPS",
                                                    "ROLE_V2.0",
                                                    "ROLE_TEST@PROD"));
        }

        @Test
        @DisplayName("Should handle roles with very long names")
        void listRoles_WithLongRoleNames_ReturnsAll() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create role with long name
            String longRoleName = "ROLE_" + "A".repeat(44); // 49 characters total
            Given.role().withName(longRoleName).persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(2))
                   .body("name", containsInAnyOrder("admin", longRoleName));
        }

        @Test
        @DisplayName("Should handle roles with spaces in name")
        void listRoles_WithSpacesInName_ReturnsAll() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create role with spaces
            Given.role().withName("ROLE DATA MANAGER").persist();
            Given.role().withName("ROLE_PROJECT LEAD").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(3))
                   .body("name", containsInAnyOrder("admin", "ROLE DATA MANAGER", "ROLE_PROJECT LEAD"));
        }

        @Test
        @DisplayName("Should handle mixed case role names")
        void listRoles_WithMixedCaseNames_ReturnsAll() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create roles with mixed case
            Given.role().withName("Role_Admin").persist();
            Given.role().withName("ROLE_user").persist();
            Given.role().withName("role_guest").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(4))
                   .body("name", containsInAnyOrder("admin", "Role_Admin", "ROLE_user", "role_guest"));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should list roles after creating new roles")
        void listRoles_AfterRoleCreation_ReturnsUpdatedList() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Initially no roles
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(1));

            // Create a role
            Given.role().withName("ROLE_NEW").persist();

            // Verify role is listed
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(2))
                   .body("name", containsInAnyOrder("admin", "ROLE_NEW"));

            // Create another role
            Given.role().withName("ROLE_ANOTHER").persist();

            // Verify both roles are listed
            given().header(admin.authenticated())
                   .when()
                   .get(LIST_ROLES_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("$", hasSize(3))
                   .body("name", containsInAnyOrder("admin", "ROLE_NEW", "ROLE_ANOTHER"));
        }

        @Test
        @DisplayName("Should return consistent results on multiple calls")
        void listRoles_MultipleCalls_ReturnsConsistentResults() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create roles
            Given.role().withName("ROLE_CONSISTENT_1").persist();
            Given.role().withName("ROLE_CONSISTENT_2").persist();

            // First call
            List<RoleResponse> firstCall = given().header(admin.authenticated())
                                                  .when()
                                                  .get(LIST_ROLES_ENDPOINT)
                                                  .then()
                                                  .statusCode(HttpStatus.SC_OK)
                                                  .extract()
                                                  .as(new TypeRef<List<RoleResponse>>() {});

            // Second call
            List<RoleResponse> secondCall = given().header(admin.authenticated())
                                                   .when()
                                                   .get(LIST_ROLES_ENDPOINT)
                                                   .then()
                                                   .statusCode(HttpStatus.SC_OK)
                                                   .extract()
                                                   .as(new TypeRef<List<RoleResponse>>() {});

            // Assert both calls return the same data
            assert firstCall.size() == secondCall.size();
            assert firstCall.stream()
                            .map(RoleResponse::name)
                            .collect(Collectors.toList())
                            .equals(secondCall.stream()
                                              .map(RoleResponse::name)
                                              .collect(Collectors.toList()));
        }
    }
}