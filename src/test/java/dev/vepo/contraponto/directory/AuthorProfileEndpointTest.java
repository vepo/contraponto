package dev.vepo.contraponto.directory;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AuthorProfileEndpointTest {

    @Inject
    UserRepository userRepository;

    @Test
    void authorProfileShowsSocialLinksAndPersonJsonLd() {
        User author = Given.user()
                           .withUsername("profile-author")
                           .withEmail("profile@example.com")
                           .withName("Profile Author")
                           .withPassword("pass12345")
                           .persist();
        author.setProfileDescription("Public bio for SEO.");
        author.setWebsiteUrl("https://example.dev");
        author.setGithubUrl("https://github.com/profile-author");
        userRepository.update(author);

        Given.post()
             .withTitle("Profile Post")
             .withSlug("profile-post")
             .withContent("Body")
             .withAuthor(author)
             .persist();

        given().when()
               .get("/authors/profile-author")
               .then()
               .statusCode(200)
               .body(containsString("https://github.com/profile-author"))
               .body(containsString("\"@type\":\"Person\""))
               .body(containsString("\"sameAs\""));
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
    }
}
