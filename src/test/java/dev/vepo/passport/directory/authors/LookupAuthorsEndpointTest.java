package dev.vepo.passport.directory.authors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.vepo.passport.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@DisplayName("Lookup public authors API")
class LookupAuthorsEndpointTest {

    @BeforeEach
    void cleanup() {
        Given.cleanup();
    }

    @Test
    @DisplayName("Should return public author fields without email")
    void shouldReturnPublicAuthorFieldsWithoutEmail() {
        var author = Given.user()
                          .withEmail("author@passport.vepo.dev")
                          .withName("Author Name")
                          .withUsername("author1")
                          .withPassword("encryptedPassword123")
                          .persist();

        // set description via update me
        given().header(author.authenticated())
               .contentType(ContentType.JSON)
               .body("""
                     {"name":"Author Name","email":"author@passport.vepo.dev","description":"Teacher bio"}
                     """)
               .when()
               .put("/api/auth/me")
               .then()
               .statusCode(HttpStatus.SC_OK);

        var caller = Given.user()
                          .withEmail("caller@passport.vepo.dev")
                          .withName("Caller")
                          .withUsername("caller1")
                          .withPassword("encryptedPassword123")
                          .persist();

        given().header(caller.authenticated())
               .contentType(ContentType.JSON)
               .body("{\"ids\":[%d,%d,%d]}".formatted(author.id(), author.id(), 99999L))
               .when()
               .post("/api/directory/authors")
               .then()
               .statusCode(HttpStatus.SC_OK)
               .body("$", hasSize(1))
               .body("[0].id", is(author.id().intValue()))
               .body("[0].name", is("Author Name"))
               .body("[0].description", is("Teacher bio"))
               .body("[0].username", is("author1"));
    }
}
