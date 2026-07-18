package dev.vepo.passport.directory.search;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@DisplayName("Search Directory Users Endpoint")
class SearchDirectoryUsersEndpointTest {

    private static final String PATH = "/api/directory/users";

    private Given.GivenUser teacher;
    private Given.GivenUser student;

    @BeforeEach
    void setUp() {
        Given.cleanup();
        Given.role().withName("USER").persist();
        Given.profile().withName("Regular User").withRole("USER").persist();

        teacher = Given.user()
                       .withUsername("teach.alice")
                       .withEmail("alice.teacher@example.com")
                       .withName("Alice Teacher")
                       .withPassword("Password123")
                       .withProfile("Regular User")
                       .persist();

        student = Given.user()
                       .withUsername("stud.bob")
                       .withEmail("bob.student@example.com")
                       .withName("Bob Student")
                       .withPassword("Password123")
                       .withProfile("Regular User")
                       .persist();

        Given.user()
             .withUsername("gone.user")
             .withEmail("gone@example.com")
             .withName("Gone User")
             .withPassword("Password123")
             .withProfile("Regular User")
             .withDisabled(true)
             .persist();
    }

    @Test
    @DisplayName("shouldRejectUnauthenticatedDirectorySearch")
    void shouldRejectUnauthenticatedDirectorySearch() {
        given().queryParam("q", "alice")
               .when()
               .get(PATH)
               .then()
               .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("shouldRejectShortDirectoryQuery")
    void shouldRejectShortDirectoryQuery() {
        given().header(teacher.authenticated())
               .queryParam("q", "a")
               .when()
               .get(PATH)
               .then()
               .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @DisplayName("shouldReturnActiveUsersMatchingQueryWithoutAdminRole")
    void shouldReturnActiveUsersMatchingQueryWithoutAdminRole() {
        given().header(teacher.authenticated())
               .queryParam("q", "bob")
               .when()
               .get(PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("items", hasSize(greaterThanOrEqualTo(1)))
               .body("items.username", hasItem(student.user().getUsername()))
               .body("items[0]", hasKey("id"))
               .body("items[0]", hasKey("username"))
               .body("items[0]", hasKey("name"))
               .body("items[0]", hasKey("email"))
               .body("items[0]", not(hasKey("profiles")))
               .body("items[0]", not(hasKey("disabled")))
               .body("total", greaterThanOrEqualTo(1))
               .body("page", equalTo(0));
    }

    @Test
    @DisplayName("shouldExcludeDisabledUsersFromDirectory")
    void shouldExcludeDisabledUsersFromDirectory() {
        given().header(teacher.authenticated())
               .queryParam("q", "gone")
               .when()
               .get(PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("items", hasSize(0))
               .body("total", equalTo(0));
    }

    @Test
    @DisplayName("shouldMatchDirectoryUsersByEmail")
    void shouldMatchDirectoryUsersByEmail() {
        given().header(student.authenticated())
               .queryParam("q", "alice.teacher")
               .when()
               .get(PATH)
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("items.email", hasItem("alice.teacher@example.com"))
               .body("items.email", everyItem(not(equalTo("gone@example.com"))));
    }
}
