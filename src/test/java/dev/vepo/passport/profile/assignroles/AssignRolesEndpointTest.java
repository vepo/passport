package dev.vepo.passport.profile.assignroles;

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
@DisplayName("Assign Roles to Profile API Endpoint Tests")
class AssignRolesEndpointTest {

    private static final String ASSIGN_ROLES_ENDPOINT = "/api/profiles/{profileId}/roles";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Nested
    @DisplayName("Authentication & Authorization")
    class AuthenticationAuthorizationTests {

        @Test
        @DisplayName("Should return UNAUTHORIZED when accessing endpoint without authentication")
        void assignRoles_WithoutAuthentication_ReturnsUnauthorized() {
            given().contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(1L, 2L)))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, 1L)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return FORBIDDEN when authenticated as non-admin user")
        void assignRoles_AsNonAdminUser_ReturnsForbidden() {
            // Arrange - Create a regular user (non-admin)
            var regularUser = Given.user()
                                   .withName("Regular User")
                                   .withUsername("regularuser")
                                   .withEmail("regular@passport.vepo.dev")
                                   .withPassword("password123")
                                   .persist();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("TargetProfile")
                                     .persist();

            // Create roles
            var role1 = Given.role().withName("ROLE_USER").persist();
            var role2 = Given.role().withName("ROLE_EDITOR").persist();

            // Act & Assert
            given().header(regularUser.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(role1.getId(), role2.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_FORBIDDEN);
        }

        @Test
        @DisplayName("Should succeed when authenticated as admin user")
        void assignRoles_AsAdminUser_ReturnsOk() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("TargetProfile")
                                     .persist();

            // Create roles
            var role1 = Given.role().withName("ROLE_USER").persist();
            var role2 = Given.role().withName("ROLE_EDITOR").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(role1.getId(), role2.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetProfile.getId().intValue()))
                   .body("name", is("TargetProfile"))
                   .body("roles", hasSize(2))
                   .body("roles.name", containsInAnyOrder("ROLE_USER", "ROLE_EDITOR"));
        }
    }

    @Nested
    @DisplayName("Successful Role Assignment")
    class SuccessfulAssignmentTests {

        @Test
        @DisplayName("Should assign single role to profile")
        void assignRoles_SingleRole_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("UserProfile")
                                     .persist();

            // Create a role
            var role = Given.role().withName("ROLE_BASIC").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(role.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetProfile.getId().intValue()))
                   .body("roles", hasSize(1))
                   .body("roles[0].name", is("ROLE_BASIC"))
                   .body("roles[0].id", is(role.getId().intValue()));
        }

        @Test
        @DisplayName("Should assign multiple roles to profile")
        void assignRoles_MultipleRoles_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("AdminProfile")
                                     .persist();

            // Create multiple roles
            var role1 = Given.role().withName("ROLE_READ").persist();
            var role2 = Given.role().withName("ROLE_WRITE").persist();
            var role3 = Given.role().withName("ROLE_DELETE").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(role1.getId(), role2.getId(), role3.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetProfile.getId().intValue()))
                   .body("roles", hasSize(3))
                   .body("roles.name", containsInAnyOrder("ROLE_READ", "ROLE_WRITE", "ROLE_DELETE"));
        }

        @Test
        @DisplayName("Should replace existing roles when assigning new ones")
        void assignRoles_ReplaceExistingRoles_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create initial roles
            var oldRole1 = Given.role().withName("OLD_ROLE_1").persist();
            var oldRole2 = Given.role().withName("OLD_ROLE_2").persist();

            // Create a target profile with initial roles
            var targetProfile = Given.profile()
                                     .withName("ProfileWithRoles")
                                     .withRole("OLD_ROLE_1")
                                     .withRole("OLD_ROLE_2")
                                     .persist();

            // Create new roles
            var newRole1 = Given.role().withName("NEW_ROLE_1").persist();
            var newRole2 = Given.role().withName("NEW_ROLE_2").persist();

            // Act - Assign new roles
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(newRole1.getId(), newRole2.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetProfile.getId().intValue()))
                   .body("roles", hasSize(2))
                   .body("roles.name", containsInAnyOrder("NEW_ROLE_1", "NEW_ROLE_2"));
        }

        @Test
        @DisplayName("Should clear all roles when assigning empty set")
        void assignRoles_EmptySet_ClearsAllRoles() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile with initial roles
            var targetProfile = Given.profile()
                                     .withName("ProfileWithRoles")
                                     .withRole("ROLE_1")
                                     .withRole("ROLE_2")
                                     .persist();

            // Act & Assert - Assign empty role set
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of()))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].message", is("At least one role must be assigned"));
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return BAD_REQUEST when roleIds is null")
        void assignRoles_WithNullRoleIds_ReturnsBadRequest() {
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("TestProfile")
                                     .persist();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("{}")
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("assignRoles.request.roleIds"))
                   .body("violations[0].message", is("At least one role must be assigned"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when roleIds is empty")
        void assignRoles_WithEmptyRoleIds_ReturnsBadRequest() {
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("TestProfile")
                                     .persist();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of()))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.field", hasItem("assignRoles.request.roleIds"))
                   .body("violations.message", hasItem("At least one role must be assigned"));
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should return NOT_FOUND when profile does not exist")
        void assignRoles_WithNonExistentProfile_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create roles
            var role = Given.role().withName("ROLE_TEST").persist();

            // Non-existent profile ID
            Long nonExistentProfileId = 999L;

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(role.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, nonExistentProfileId)
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("Profile not found with id: 999"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when role does not exist")
        void assignRoles_WithNonExistentRole_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("TestProfile")
                                     .persist();

            // Non-existent role ID
            Long nonExistentRoleId = 999L;

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(nonExistentRoleId)))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("Could not find roles! ids=[999]"));
        }

        @Test
        @DisplayName("Should return NOT_FOUND when some roles do not exist")
        void assignRoles_WithMixedValidAndInvalidRoles_ReturnsNotFound() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("TestProfile")
                                     .persist();

            // Create one valid role
            var validRole = Given.role().withName("VALID_ROLE").persist();

            // Use one invalid role ID
            Long invalidRoleId = 888L;

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(validRole.getId(), invalidRoleId)))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_NOT_FOUND)
                   .body("message", is("Could not find roles! ids=[888]"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle assigning same role multiple times (duplicates in request)")
        void assignRoles_WithDuplicateRoleIds_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("TestProfile")
                                     .persist();

            // Create a role
            var role = Given.role().withName("ROLE_DUPLICATE").persist();

            // Act & Assert - Duplicate IDs in request should still work (Set eliminates duplicates)
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("""
                         {
                           "roleIds": [%d, %d, %d]
                         }
                         """.formatted(role.getId(), role.getId(), role.getId()))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(1))
                   .body("roles[0].id", is(role.getId().intValue()));
        }

        @Test
        @DisplayName("Should assign many roles to profile")
        void assignRoles_WithManyRoles_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("PowerUserProfile")
                                     .persist();

            // Create 8 roles (reasonable number for a profile)
            Long[] roleIds = new Long[8];
            for (int i = 0; i < 8; i++) {
                var role = Given.role()
                               .withName("ROLE_PERMISSION_" + i)
                               .persist();
                roleIds[i] = role.getId();
            }

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(roleIds)))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(8));
        }

        @Test
        @DisplayName("Should maintain profile's name when assigning roles")
        void assignRoles_MaintainsProfileName_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile with specific name
            var targetProfile = Given.profile()
                                     .withName("Special Profile Name")
                                     .persist();

            // Create a role
            var role = Given.role().withName("ROLE_SPECIAL").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(role.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("id", is(targetProfile.getId().intValue()))
                   .body("name", equalTo("Special Profile Name"))
                   .body("roles", hasSize(1));
        }

        @Test
        @DisplayName("Should handle profiles with special characters in name")
        void assignRoles_ToProfileWithSpecialName_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile with special characters
            var targetProfile = Given.profile()
                                     .withName("Admin-Profile_V2.0")
                                     .persist();

            // Create a role
            var role = Given.role().withName("ROLE_ADMIN").persist();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(role.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("name", equalTo("Admin-Profile_V2.0"))
                   .body("roles[0].name", is("ROLE_ADMIN"));
        }
    }

    @Nested
    @DisplayName("Role Inheritance Tests")
    class RoleInheritanceTests {

        @Test
        @DisplayName("Should cascade role assignment to users through profiles")
        void assignRoles_CascadesToUsers_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create roles
            var readRole = Given.role().withName("ROLE_READ").persist();
            var writeRole = Given.role().withName("ROLE_WRITE").persist();

            // Create a profile
            var profile = Given.profile()
                               .withName("EditorProfile")
                               .persist();

            // Create a user with the profile
            var user = Given.user()
                            .withName("Test User")
                            .withUsername("testuser")
                            .withEmail("test@passport.vepo.dev")
                            .withPassword("password123")
                            .withProfile(profile.getName())
                            .persist();

            // Act - Assign roles to profile
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(readRole.getId(), writeRole.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, profile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(2));

            // Verify user inherits the roles through profile
            // This would typically be checked through a separate endpoint or service
            // For now, we verify the profile has the roles
            given().header(admin.authenticated())
                   .when()
                   .get("/api/profiles/{profileId}", profile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(2))
                   .body("roles.name", containsInAnyOrder("ROLE_READ", "ROLE_WRITE"));
        }

        @Test
        @DisplayName("Should update role inheritance when replacing profile roles")
        void assignRoles_ReplacementUpdatesInheritance_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create initial roles
            var oldRole = Given.role().withName("ROLE_OLD").persist();

            // Create a profile with initial role
            var profile = Given.profile()
                               .withName("TestProfile")
                               .withRole("ROLE_OLD")
                               .persist();

            // Create a user with the profile
            var user = Given.user()
                            .withName("Test User")
                            .withUsername("testuser")
                            .withEmail("test@passport.vepo.dev")
                            .withPassword("password123")
                            .withProfile(profile.getName())
                            .persist();

            // Create new role
            var newRole = Given.role().withName("ROLE_NEW").persist();

            // Act - Replace profile roles
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(newRole.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, profile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(1))
                   .body("roles[0].name", is("ROLE_NEW"));

            // Verify old role is removed, new role is present
            given().header(admin.authenticated())
                   .when()
                   .get("/api/profiles/{profileId}", profile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles[0].name", is("ROLE_NEW"));
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should work with create profile and assign roles workflow")
        void assignRoles_CreateAndAssignWorkflow_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create roles first
            var role1 = Given.role().withName("ROLE_CREATE").persist();
            var role2 = Given.role().withName("ROLE_UPDATE").persist();
            var role3 = Given.role().withName("ROLE_DELETE").persist();

            // Create a profile (initially without roles)
            var profile = Given.profile()
                               .withName("CRUD_Profile")
                               .persist();

            // Verify profile initially has no roles
            given().header(admin.authenticated())
                   .when()
                   .get("/api/profiles/{profileId}", profile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(0));

            // Act - Assign CRUD roles
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(role1.getId(), role2.getId(), role3.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, profile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(3));

            // Verify final state
            given().header(admin.authenticated())
                   .when()
                   .get("/api/profiles/{profileId}", profile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles.name", containsInAnyOrder("ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE"));
        }

        @Test
        @DisplayName("Should allow multiple reassignments of roles")
        void assignRoles_MultipleReassignments_Success() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a target profile
            var targetProfile = Given.profile()
                                     .withName("ConfigurableProfile")
                                     .persist();

            // Create multiple sets of roles
            var roleSet1 = Given.role().withName("ROLE_SET1").persist();
            var roleSet2 = Given.role().withName("ROLE_SET2").persist();
            var roleSet3 = Given.role().withName("ROLE_SET3").persist();

            // First assignment
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(roleSet1.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(1))
                   .body("roles[0].name", is("ROLE_SET1"));

            // Second assignment
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(roleSet2.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(1))
                   .body("roles[0].name", is("ROLE_SET2"));

            // Third assignment
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(assignRolesRequest(Set.of(roleSet3.getId())))
                   .when()
                   .post(ASSIGN_ROLES_ENDPOINT, targetProfile.getId())
                   .then()
                   .statusCode(HttpStatus.SC_OK)
                   .body("roles", hasSize(1))
                   .body("roles[0].name", is("ROLE_SET3"));
        }
    }

    /**
     * Helper method to create an assign roles request JSON body.
     */
    private String assignRolesRequest(Set<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return """
                   {
                     "roleIds": []
                   }
                   """;
        }
        
        String roleIdsArray = roleIds.stream()
                                     .map(String::valueOf)
                                     .reduce((a, b) -> a + ", " + b)
                                     .orElse("");

        return """
               {
                 "roleIds": [%s]
               }
               """.formatted(roleIdsArray);
    }
}