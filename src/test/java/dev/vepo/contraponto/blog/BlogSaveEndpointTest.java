package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class BlogSaveEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogRepository blogRepository;

    @Inject
    EntityManager entityManager;

    private User owner;

    @Test
    void create_blog_returns_success_toast_headers() {
        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded")
                .formParam("name", "Travel Blog")
                .formParam("slug", "travel")
                .formParam("description", "Trips")
                .formParam("active", "on")
                .post("/forms/blogs")
                .then()
                .statusCode(200)
                .header("X-Toast-Message", equalTo("Blog saved successfully."))
                .header("X-Toast-Type", equalTo("Success"))
                .header("HX-Trigger",
                        equalTo("{\"toast:show\":{\"i18nKey\":\"toast.blog.saved\",\"message\":\"Blog saved successfully.\",\"duration\":10000,\"type\":\"Success\"}}"))
                .header("HX-Trigger-After-Settle",
                        equalTo("{\"toast:show\":{\"i18nKey\":\"toast.blog.saved\",\"message\":\"Blog saved successfully.\",\"duration\":10000,\"type\":\"Success\"}}"));
    }

    @Test
    void createBlogWithProfilePictureRendersSuccessPage() {
        var picture = Given.randomCover(owner.getDefaultBlog());
        Given.transaction(() -> {
            var managed = entityManager.find(User.class, owner.getId());
            managed.setProfilePicture(picture);
        });

        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded")
                .formParam("name", "Photo Blog")
                .formParam("slug", "photo")
                .formParam("description", "With avatar")
                .formParam("active", "on")
                .post("/forms/blogs")
                .then()
                .statusCode(200)
                .header("X-Toast-Type", equalTo("Success"))
                .body(containsString(picture.getUrl()));
    }

    @Test
    void mainBlogSaveAppliesGitIntegration() {
        var mainBlog = owner.getDefaultBlog();
        var remoteUrl = "https://github.com/vepo/contraponto.git";

        TestHttp.authenticated(owner)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .formParam("id", mainBlog.getId())
                .formParam("name", "Main Blog With Git")
                .formParam("slug", mainBlog.getSlug())
                .formParam("description", "Default blog bio")
                .formParam("hub", "writing")
                .formParam("git_enabled", "on")
                .formParam("git_remote_url", remoteUrl)
                .formParam("git_branch", "main")
                .post("/forms/blogs")
                .then()
                .statusCode(200);

        var reloaded = Given.transaction(() -> {
            entityManager.clear();
            return blogRepository.findById(mainBlog.getId()).orElseThrow();
        });
        assertThat(reloaded.getName()).isEqualTo("Main Blog With Git");
        assertThat(reloaded.isGitEnabled()).isTrue();
        assertThat(reloaded.getGitRemoteUrl()).isEqualTo(remoteUrl);
        assertThat(reloaded.getGitBranch()).isEqualTo("main");
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        owner = Given.user()
                     .withUsername("blogsave")
                     .withEmail("blogsave@example.com")
                     .withPassword("blogSavePw1")
                     .withName("Blog Save Owner")
                     .persist();
    }
}
