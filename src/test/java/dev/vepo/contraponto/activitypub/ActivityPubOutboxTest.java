package dev.vepo.contraponto.activitypub;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;

@QuarkusIntegrationTest
class ActivityPubOutboxTest {

    private static final class RestAssuredPort {
        static void configure(URL baseUrl) {
            io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
            io.restassured.RestAssured.port = baseUrl.getPort();
            io.restassured.RestAssured.basePath = "";
        }

        private RestAssuredPort() {}
    }

    @TestHTTPResource("/")
    URL baseUrl;

    private User user;

    private Post post;

    @Test
    void actorJsonIncludesProfilePictureIcon() {
        var picture = Given.randomCover(user.getDefaultBlog());
        Given.transaction(() -> {
            var managed = Given.inject(jakarta.persistence.EntityManager.class).find(User.class, user.getId());
            managed.setProfilePicture(picture);
        });

        var json = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                          .get("/outboxuser")
                          .then()
                          .statusCode(200)
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"icon\"")
                        .contains("\"type\":\"Image\"")
                        .contains("\"mediaType\":\"image/png\"")
                        .contains(picture.getUrl());
    }

    @Test
    void actorJsonReturnsPersonWithInboxAndOutbox() {
        var json = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                          .get("/outboxuser")
                          .then()
                          .statusCode(200)
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"type\":\"Person\"")
                        .contains("\"inbox\"")
                        .contains("\"outbox\"")
                        .contains("\"discoverable\":true")
                        .contains("\"webfinger\":\"outboxuser@")
                        .contains("outboxuser")
                        .doesNotContain("\"icon\"");
    }

    @Test
    void createActivityIsFetchableById() {
        var json = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                          .get("/outboxuser/activities/create/%d".formatted(post.getId()))
                          .then()
                          .statusCode(200)
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"type\":\"Create\"")
                        .contains("Outbox Post")
                        .contains("/activities/create/%d".formatted(post.getId()));
    }

    @Test
    void disabledActorReturns404() {
        Given.activityPubActor().withUser(user).withFederationEnabled(false).persist();
        given().accept(ActivityPubPaths.ACTIVITY_JSON)
               .get("/outboxuser/outbox")
               .then()
               .statusCode(404);
        given().accept(ActivityPubPaths.ACTIVITY_JSON)
               .get("/outboxuser")
               .then()
               .statusCode(404);
    }

    @Test
    void outboxExposesPaginationLinksAcrossPages() {
        for (var index = 2; index <= 21; index++) {
            Given.post()
                 .withAuthor(user)
                 .withTitle("Paged Post %d".formatted(index))
                 .withSlug("paged-post-%d".formatted(index))
                 .withDescription("Summary")
                 .withContent("Body")
                 .persist();
        }

        var firstPage = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                               .get("/outboxuser/outbox")
                               .then()
                               .statusCode(200)
                               .extract()
                               .body()
                               .asString();
        assertThat(firstPage).contains("\"totalItems\":21")
                             .contains("\"first\"")
                             .contains("\"last\"")
                             .contains("\"next\"")
                             .doesNotContain("\"prev\"");

        var secondPage = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                                .get("/outboxuser/outbox?page=2")
                                .then()
                                .statusCode(200)
                                .extract()
                                .body()
                                .asString();
        assertThat(secondPage).contains("\"type\":\"OrderedCollectionPage\"")
                              .contains("\"partOf\"")
                              .contains("\"prev\"")
                              .doesNotContain("\"next\"");
    }

    @Test
    void outboxIncludesSecondaryBlogCreateWithBlogNameAndPath() {
        var secondary = Given.blog()
                             .withUser(user)
                             .withSlug("lab-notes")
                             .withName("Lab Notes")
                             .withDescription("Secondary blog for Fediverse outbox")
                             .persist();
        var secondaryPost = Given.post()
                                 .withAuthor(user)
                                 .withBlog(secondary)
                                 .withTitle("Secondary Outbox Note")
                                 .withSlug("secondary-outbox-note")
                                 .withDescription("Must not appear in Create content")
                                 .withContent("Body")
                                 .persist();

        var outbox = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                            .get("/outboxuser/outbox")
                            .then()
                            .statusCode(200)
                            .extract()
                            .body()
                            .asString();
        assertThat(outbox).contains("Outbox Post")
                          .contains("Secondary Outbox Note")
                          .contains("Lab Notes")
                          .contains("/outboxuser/lab-notes/post/secondary-outbox-note")
                          .contains("\"published\"");

        var activity = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                              .get("/outboxuser/activities/create/%d".formatted(secondaryPost.getId()))
                              .then()
                              .statusCode(200)
                              .extract()
                              .body()
                              .asString();
        assertThat(activity).contains("\"type\":\"Create\"")
                            .contains("Secondary Outbox Note")
                            .contains("Lab Notes")
                            .contains("/outboxuser/lab-notes/post/secondary-outbox-note")
                            .contains("\"published\"")
                            .doesNotContain("Must not appear in Create content");
    }

    @Test
    void outboxListsCreateAfterPublish() {
        var json = given().accept(ActivityPubPaths.ACTIVITY_JSON)
                          .get("/outboxuser/outbox")
                          .then()
                          .statusCode(200)
                          .extract()
                          .body()
                          .asString();
        assertThat(json).contains("\"type\":\"OrderedCollection\"")
                        .contains("\"type\":\"Create\"")
                        .contains("Outbox Post")
                        .doesNotContain("//activities/");
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        RestAssuredPort.configure(baseUrl);
        user = Given.user()
                    .withUsername("outboxuser")
                    .withEmail("outboxuser@example.com")
                    .withPassword("pw123456789")
                    .withName("Outbox User")
                    .persist();
        Given.activityPubActor().withUser(user).persist();
        post = Given.post()
                    .withAuthor(user)
                    .withTitle("Outbox Post")
                    .withSlug("outbox-post")
                    .withDescription("Summary")
                    .withContent("Body")
                    .persist();
    }
}
