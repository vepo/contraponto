package dev.vepo.contraponto.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublicationService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class PostPublishedNotificationTest {

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    BlogAudienceRepository audienceRepository;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    EmailNotificationLogRepository emailLogRepository;

    @Inject
    BlogRepository blogRepository;

    @Inject
    PostRepository postRepository;

    @Inject
    PostPublicationService publicationService;

    @Inject
    MockMailbox mailbox;

    private User author;
    private User follower;
    private User subscriber;
    private long blogId;

    @Test
    void content_change_republish_notifies_again() {
        Post post = Given.post()
                         .withAuthor(author)
                         .withTitle("Evolving")
                         .withContent("V1")
                         .withSlug("evolving-notify")
                         .withPublished(true)
                         .persist();

        long afterFirst = notificationRepository.findPage(follower.getId(),
                                                          new dev.vepo.contraponto.shared.pagination.PageQuery(20, 0, 20, 1))
                                                .total();
        assertThat(afterFirst).isEqualTo(1);

        mailbox.clear();

        Post loaded = postRepository.findByIdWithTags(post.getId()).orElseThrow();
        loaded.setContent("V2");
        postRepository.save(loaded);
        publicationService.publish(postRepository.findByIdWithTags(post.getId()).orElseThrow());

        long afterSecond = notificationRepository.findPage(follower.getId(),
                                                           new dev.vepo.contraponto.shared.pagination.PageQuery(20, 0, 20, 1))
                                                 .total();
        assertThat(afterSecond).isEqualTo(2);
        assertThat(mailbox.getMailsSentTo("pubsubscriber@test.com")).hasSize(1);
    }

    @Test
    void identical_republish_does_not_duplicate_notifications() {
        Post post = Given.post()
                         .withAuthor(author)
                         .withTitle("Stable")
                         .withContent("Same")
                         .withSlug("stable-notify")
                         .withPublished(true)
                         .persist();

        mailbox.clear();

        long before = notificationRepository.findPage(follower.getId(),
                                                      new dev.vepo.contraponto.shared.pagination.PageQuery(20, 0, 20, 1))
                                            .total();

        publicationService.publish(postRepository.findByIdWithTags(post.getId()).orElseThrow());

        long after = notificationRepository.findPage(follower.getId(),
                                                     new dev.vepo.contraponto.shared.pagination.PageQuery(20, 0, 20, 1))
                                           .total();

        assertThat(after).isEqualTo(before);
        assertThat(mailbox.getMailsSentTo("pubsubscriber@test.com")).isEmpty();
    }

    @Test
    void publish_creates_notification_and_email() {
        Post post = Given.post()
                         .withAuthor(author)
                         .withTitle("Hello")
                         .withContent("Body")
                         .withSlug("hello-notify")
                         .withDescription("Excerpt")
                         .persist();

        Given.inject(PostPublicationService.class).publish(postRepository.findByIdWithTags(post.getId()).orElseThrow());

        assertThat(notificationRepository.findPage(follower.getId(),
                                                   new dev.vepo.contraponto.shared.pagination.PageQuery(20, 0, 20, 1))
                                         .data())
                                                 .anyMatch(n -> n.getType() == NotificationType.NEW_POST);

        assertThat(mailbox.getMailsSentTo("pubsubscriber@test.com")).hasSize(1);

        var publicationId = postRepository.findByIdWithTags(post.getId()).orElseThrow().getLivePublication().getId();
        assertThat(emailLogRepository.exists(publicationId, subscriber.getId())).isTrue();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        mailbox.clear();

        author = Given.user()
                      .withUsername("pubauthor")
                      .withEmail("pubauthor@test.com")
                      .withName("Pub Author")
                      .withPassword("password123")
                      .persist();
        follower = Given.user()
                        .withUsername("pubfollower")
                        .withEmail("pubfollower@test.com")
                        .withName("Pub Follower")
                        .withPassword("password123")
                        .persist();
        subscriber = Given.user()
                          .withUsername("pubsubscriber")
                          .withEmail("pubsubscriber@test.com")
                          .withName("Pub Subscriber")
                          .withPassword("password123")
                          .persist();
        blogId = blogRepository.findMainByOwnerId(author.getId()).orElseThrow().getId();

        TestHttp.authenticated(follower)
                .post("/forms/blogs/" + blogId + "/follow");

        TestHttp.authenticated(subscriber)
                .post("/forms/blogs/" + blogId + "/subscribe");
    }
}
