package dev.vepo.passport.role.create;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("Create Role API Endpoint Tests")
class CreateRoleEndpointTest {

    private static final String CREATE_ROLE_ENDPOINT = "/api/roles";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Nested
    @DisplayName("Authentication & Authorization")
    class AuthenticationAuthorizationTests {

        @Test
        @DisplayName("Should return UNAUTHORIZED when accessing endpoint without authentication")
        void createRole_WithoutAuthentication_ReturnsUnauthorized() {
            given().contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE_USER"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should return FORBIDDEN when authenticated as non-admin user")
        void createRole_AsNonAdminUser_ReturnsForbidden() {
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
                   .body(createRoleRequest("ROLE_USER"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_FORBIDDEN);
        }

        @Test
        @DisplayName("Should succeed when authenticated as admin user")
        void createRole_AsAdminUser_ReturnsCreated() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE_MANAGER"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", is("ROLE_MANAGER"))
                   .body("id", notNullValue());
        }
    }

    @Nested
    @DisplayName("Successful Role Creation")
    class SuccessfulCreationTests {

        @Test
        @DisplayName("Should create role with valid request and return created role")
        void createRole_WithValidRequest_ReturnsCreatedRole() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE_SUPERVISOR"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", is("ROLE_SUPERVISOR"))
                   .body("id", notNullValue());
        }

        @Test
        @DisplayName("Should create role with minimum valid name length")
        void createRole_WithMinimumNameLength_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert - Name with exactly 3 characters (minimum)
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ADM"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", is("ADM"));
        }

        @Test
        @DisplayName("Should create role with maximum valid name length")
        void createRole_WithMaximumNameLength_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Role name with exactly 50 characters (maximum)
            String maxLengthName = "A".repeat(50);

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest(maxLengthName))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", is(maxLengthName));
        }

        @Test
        @DisplayName("Should create role with special characters in name")
        void createRole_WithSpecialCharacters_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE_DATA_ANALYST_V2"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", is("ROLE_DATA_ANALYST_V2"));
        }
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return BAD_REQUEST when name is null")
        void createRole_WithNullName_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body("{}")
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'create.request.name' }.message", is("must not be blank"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when name is empty")
        void createRole_WithEmptyName_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest(""))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.findAll{ it.field == 'create.request.name' && it.message == 'must not be blank' }.size()", is(1));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when name contains only whitespace")
        void createRole_WithBlankName_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("   "))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'create.request.name' }.message", is("must not be blank"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when name is too short")
        void createRole_WithShortName_ReturnsBadRequest() {
            var admin = Given.admin();

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("AB"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'create.request.name' }.message", is("size must be between 3 and 50"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when name is too long")
        void createRole_WithLongName_ReturnsBadRequest() {
            var admin = Given.admin();

            String longName = "A".repeat(51);

            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest(longName))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations.find{ it.field == 'create.request.name' }.message", is("size must be between 3 and 50"));
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should prevent duplicate role name")
        void createRole_WithExistingRoleName_ReturnsConflict() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a role first
            Given.role()
                 .withName("ROLE_EXISTING")
                 .persist();

            // Act & Assert - Try to create another role with same name
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE_EXISTING"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CONFLICT)
                   .body("message", equalTo("Role with name 'ROLE_EXISTING' already exists"));
        }

        @Test
        @DisplayName("Should prevent duplicate role name (case-insensitive)")
        void createRole_WithExistingRoleNameDifferentCase_ReturnsConflict() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a role first
            Given.role()
                 .withName("ROLE_ADMIN")
                 .persist();

            // Act & Assert - Try to create role with different case
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("role_admin"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CONFLICT);
        }

        @Test
        @DisplayName("Should create role with similar but different name")
        void createRole_WithSimilarName_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Create a role first
            Given.role()
                 .withName("ROLE_USER")
                 .persist();

            // Act & Assert - Try to create role with similar name
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE_USERS"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", is("ROLE_USERS"));
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should create role with numbers in name")
        void createRole_WithNumbersInName_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE_V2"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", equalTo("ROLE_V2"));
        }

        @Test
        @DisplayName("Should create role with underscores")
        void createRole_WithUnderscores_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE_DATA_MANAGER"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", equalTo("ROLE_DATA_MANAGER"));
        }

        @Test
        @DisplayName("Should create role with hyphens")
        void createRole_WithHyphens_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("ROLE-DATA-MANAGER"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", equalTo("ROLE-DATA-MANAGER"));
        }

        @Test
        @DisplayName("Should create role with mixed case")
        void createRole_WithMixedCase_Succeeds() {
            // Arrange - Admin authentication
            var admin = Given.admin();

            // Act & Assert
            given().header(admin.authenticated())
                   .contentType(ContentType.JSON)
                   .body(createRoleRequest("Role_MixedCase"))
                   .when()
                   .post(CREATE_ROLE_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_CREATED)
                   .body("name", equalTo("Role_MixedCase"));
        }
    }

    /**
     * Helper method to create a create role request JSON body.
     */
    private String createRoleRequest(String name) {
        return """
               {
                   "name": "%s"
               }
               """.formatted(name);
    }
}