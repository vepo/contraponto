package dev.vepo.contraponto.post;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.endsWith;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostSlugRedirectTest {

    @TestHTTPResource
    URL baseUrl;

    @Inject
    PostPublicationService publicationService;

    @Inject
    PostRepository postRepository;

    @Inject
    PostSlugAliasRepository postSlugAliasRepository;

    @Test
    void oldSlugRedirectsToCurrentUrlAfterRepublish() {
        User author = Given.user()
                           .withUsername("redirect-author")
                           .withEmail("redirect@test.com")
                           .withName("Redirect Author")
                           .withPassword("password123")
                           .persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Redirect test")
                        .withSlug("old-slug")
                        .withContent("Original body")
                        .withPublished(true)
                        .persist();

        Post reloaded = postRepository.findByIdWithTags(post.getId()).orElseThrow();
        reloaded.setSlug("new-slug");
        publicationService.publish(reloaded);

        reloaded = postRepository.findByIdWithTags(post.getId()).orElseThrow();
        assertThat(reloaded.getSlug()).isEqualTo("new-slug");
        assertThat(postSlugAliasRepository.findPostIdByBlogAndSlug(reloaded.getBlog().getId(), "old-slug"))
                                                                                                           .contains(reloaded.getId());

        given().redirects()
               .follow(false)
               .when()
               .get("/" + author.getUsername() + "/post/old-slug")
               .then()
               .statusCode(301)
               .header("Location", endsWith("/" + author.getUsername() + "/post/new-slug"));
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";
    }
}
