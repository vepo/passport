package dev.vepo.passport.auth.login;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
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
@DisplayName("Login API Endpoint Tests")
class LoginEndpointTest {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String ADMIN_EMAIL = "sysadmin@passport.vepo.dev";
    private static final String ADMIN_PASSWORD = "qwas1234";
    private static final String NON_EXISTENT_EMAIL = "admin@passport.vepo.dev";
    private static final String DELETED_USER_EMAIL = "deleted.user@passport.vepo.dev";
    private static final String DELETED_USER_PASSWORD = "encryptedPassword123";

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Test
    @DisplayName("Should return authentication token with valid credentials")
    void login_WithValidCredentials_ReturnsAuthenticationToken() {
        given().contentType(ContentType.JSON)
               .body(loginRequest(ADMIN_EMAIL, ADMIN_PASSWORD))
               .when()
               .post(LOGIN_ENDPOINT)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("token", notNullValue());
    }

    @Nested
    @DisplayName("Authentication Failure Scenarios")
    class AuthenticationFailureTests {

        @Test
        @DisplayName("Should return UNAUTHORIZED with incorrect password")
        void login_WithIncorrectPassword_ReturnsUnauthorized() {
            given().contentType(ContentType.JSON)
                   .body(loginRequest(ADMIN_EMAIL, "qwas12341"))
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED)
                   .body("status", is(HttpStatus.SC_UNAUTHORIZED))
                   .body("message", is("Invalid credentials!"));
        }

        @Test
        @DisplayName("Should return UNAUTHORIZED with non-existent email")
        void login_WithNonExistentEmail_ReturnsUnauthorized() {
            given().contentType(ContentType.JSON)
                   .body(loginRequest(NON_EXISTENT_EMAIL, ADMIN_PASSWORD))
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED)
                   .body("status", is(HttpStatus.SC_UNAUTHORIZED))
                   .body("message", is("Invalid credentials!"));
        }

        @Test
        @DisplayName("Should return UNAUTHORIZED for deleted user account")
        void login_WithDeletedUserAccount_ReturnsUnauthorized() {
            // Arrange
            Given.user()
                 .withEmail(DELETED_USER_EMAIL)
                 .withName("Deleted User")
                 .withUsername("deleted-user")
                 .withPassword(DELETED_USER_PASSWORD)
                 .withDeleted(true)
                 .persist();

            // Act & Assert
            given().contentType(ContentType.JSON)
                   .body(loginRequest(DELETED_USER_EMAIL, DELETED_USER_PASSWORD))
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_UNAUTHORIZED)
                   .body("status", is(HttpStatus.SC_UNAUTHORIZED))
                   .body("message", is("Invalid credentials!"));
        }
    }

    @Nested
    @DisplayName("Request Validation Scenarios")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return BAD_REQUEST when email is missing")
        void login_WithoutEmail_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("{ \"password\": \"qwas1234\" }")
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("status", is(HttpStatus.SC_BAD_REQUEST))
                   .body("title", is("Constraint Violation"))
                   .body("violations[0].field", is("login.request.email"))
                   .body("violations[0].message", is("Email must not be empty!"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when password is missing")
        void login_WithoutPassword_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("{ \"email\": \"user@example.com\" }")
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("status", is(HttpStatus.SC_BAD_REQUEST))
                   .body("title", is("Constraint Violation"))
                   .body("violations[0].field", is("login.request.password"))
                   .body("violations[0].message", is("Password must not be empty!"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when password is empty")
        void login_WithEmptyPassword_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(loginRequest("user@example.com", ""))
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST when password contains only whitespace")
        void login_WithWhitespacePassword_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(loginRequest("user@example.com", "   "))
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with invalid email format")
        void login_WithInvalidEmailFormat_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(loginRequest("not-an-email", "password123"))
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST)
                   .body("violations[0].field", is("login.request.email"))
                   .body("violations[0].message", containsString("valid email"));
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with empty email string")
        void login_WithEmptyEmail_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body(loginRequest("", "password123"))
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }

        @Test
        @DisplayName("Should return BAD_REQUEST with malformed JSON")
        void login_WithMalformedJson_ReturnsBadRequest() {
            given().contentType(ContentType.JSON)
                   .body("{ malformed json }")
                   .when()
                   .post(LOGIN_ENDPOINT)
                   .then()
                   .statusCode(HttpStatus.SC_BAD_REQUEST);
        }
    }

    /**
     * Helper method to create a login request JSON body.
     */
    private String loginRequest(String email, String password) {
        return String.format("""
                             {
                                 "email": "%s",
                                 "password": "%s"
                             }
                             """, email, password);
    }
}