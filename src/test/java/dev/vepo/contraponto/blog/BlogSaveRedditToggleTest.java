package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;

import dev.vepo.contraponto.reddit.account.UserRedditAccount;
import dev.vepo.contraponto.reddit.account.UserRedditAccountRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class BlogSaveRedditToggleTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogRepository blogRepository;

    @Inject
    UserRedditAccountRepository redditAccountRepository;

    @Inject
    EntityManager entityManager;

    private User owner;

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        owner = Given.user()
                     .withUsername("reddittoggle")
                     .withEmail("reddittoggle@example.com")
                     .withPassword("redditTogglePw1")
                     .withName("Reddit Toggle Owner")
                     .persist();
    }

    @Test
    void togglePersistsTrueWhenOwnerHasLinkedAccount() {
        var mainBlog = owner.getDefaultBlog();
        Given.transaction(() -> redditAccountRepository.create(new UserRedditAccount(owner, "coolredditor", "ciphertext")));

        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("id", mainBlog.getId())
                .formParam("name", mainBlog.getName())
                .formParam("slug", mainBlog.getSlug())
                .formParam("description", "Bio")
                .formParam("hub", "writing")
                .formParam("reddit_enabled", "on")
                .post("/forms/blogs")
                .then()
                .statusCode(200);

        var reloaded = Given.transaction(() -> {
            entityManager.clear();
            return blogRepository.findById(mainBlog.getId()).orElseThrow();
        });
        assertThat(reloaded.isRedditSyndicationEnabled()).isTrue();
    }

    @Test
    void toggleStaysFalseWhenOwnerHasNoLinkedAccount() {
        var mainBlog = owner.getDefaultBlog();

        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("id", mainBlog.getId())
                .formParam("name", mainBlog.getName())
                .formParam("slug", mainBlog.getSlug())
                .formParam("description", "Bio")
                .formParam("hub", "writing")
                .formParam("reddit_enabled", "on")
                .post("/forms/blogs")
                .then()
                .statusCode(200);

        var reloaded = Given.transaction(() -> {
            entityManager.clear();
            return blogRepository.findById(mainBlog.getId()).orElseThrow();
        });
        assertThat(reloaded.isRedditSyndicationEnabled()).isFalse();
    }
}
