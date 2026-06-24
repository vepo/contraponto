package dev.vepo.contraponto.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class BlogAudienceEndpointTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogAudienceRepository audienceRepository;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    BlogRepository blogRepository;

    private User author;
    private User follower;
    private Blog authorBlog;

    @Test
    void audience_controls_requires_active_blog() {
        session(follower).get("/components/blogs/" + authorBlog.getId() + "/audience")
                         .then()
                         .statusCode(200)
                         .body(containsString("data-i18n=\"audience.follow\""));
    }

    @Test
    void cannot_follow_own_blog() {
        session(author).post("/forms/blogs/" + authorBlog.getId() + "/follow")
                       .then()
                       .statusCode(400)
                       .header("X-Toast-Message", containsString("own blog"));
    }

    @Test
    void follow_notifies_blog_owner() {
        session(follower).post("/forms/blogs/" + authorBlog.getId() + "/follow")
                         .then()
                         .statusCode(200);

        assertThat(audienceRepository.findByUserAndBlog(follower.getId(), authorBlog.getId()))
                                                                                              .map(BlogAudience::isFollowed)
                                                                                              .contains(true);

        long ownerNotifications = notificationRepository.countUnread(author.getId());
        assertThat(ownerNotifications).isEqualTo(1);
    }

    private io.restassured.specification.RequestSpecification session(User user) {
        return TestHttp.authenticated(user);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        author = Given.user()
                      .withUsername("blogowner")
                      .withEmail("owner@test.com")
                      .withName("Blog Owner")
                      .withPassword("password123")
                      .persist();
        follower = Given.user()
                        .withUsername("follower")
                        .withEmail("follower@test.com")
                        .withName("Follower")
                        .withPassword("password123")
                        .persist();
        authorBlog = blogRepository.findMainByOwnerId(author.getId()).orElseThrow();
    }

    @Test
    void subscribe_notifies_blog_owner() {
        session(follower).post("/forms/blogs/" + authorBlog.getId() + "/subscribe")
                         .then()
                         .statusCode(200);

        assertThat(audienceRepository.findByUserAndBlog(follower.getId(), authorBlog.getId()))
                                                                                              .map(BlogAudience::isEmailSubscribed)
                                                                                              .contains(true);

        assertThat(notificationRepository.findPage(author.getId(),
                                                   new dev.vepo.contraponto.shared.pagination.PageQuery(20, 0, 20, 1))
                                         .data())
                                                 .anyMatch(n -> n.getType() == NotificationType.NEW_SUBSCRIBE);
    }
}
