package dev.vepo.contraponto.reaction;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.notification.NotificationRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestHttp;
import dev.vepo.contraponto.user.User;

@QuarkusIntegrationTest
class PostReactionTest {

    private User author;
    private User reader;
    private Post post;

    @Test
    void anonymous_reader_sees_counts_without_reaction_buttons() {
        given().get("/" + author.getUsername() + "/post/" + post.getSlug() + "/components/reactions")
               .then()
               .statusCode(200)
               .body(containsString("reaction-bar__button--static"))
               .body(not(containsString("reaction-bar__form")))
               .body(containsString("reaction.signInHint"));
    }

    @Test
    void author_cannot_react_to_own_post() {
        TestHttp.authenticated(author)
                .post("/forms/posts/" + post.getId() + "/reactions/like")
                .then()
                .statusCode(400);
    }

    @Test
    void post_page_includes_reaction_bar_container() {
        given().get("/" + author.getUsername() + "/post/" + post.getSlug())
               .then()
               .statusCode(200)
               .body(containsString("id=\"post-reactions\""))
               .body(containsString("/components/reactions"));
    }

    @Test
    void reader_can_hold_multiple_independent_reaction_types() {
        TestHttp.authenticated(reader)
                .post("/forms/posts/" + post.getId() + "/reactions/like")
                .then()
                .statusCode(200);

        TestHttp.authenticated(reader)
                .post("/forms/posts/" + post.getId() + "/reactions/rocket")
                .then()
                .statusCode(200)
                .body(containsString("reaction-bar__button--active"));

        long activeCount = Given.inject(PostReactionRepository.class).findActiveTypes(post.getId(), reader.getId()).size();
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    void reader_toggle_adds_then_removes_reaction_and_notifies_author_once() {
        long unreadBefore = Given.inject(NotificationRepository.class).countUnread(author.getId());

        TestHttp.authenticated(reader)
                .post("/forms/posts/" + post.getId() + "/reactions/like")
                .then()
                .statusCode(200)
                .body(containsString("reaction-bar__button--active"));

        long unreadAfterAdd = Given.inject(NotificationRepository.class).countUnread(author.getId());
        assertThat(unreadAfterAdd).isEqualTo(unreadBefore + 1);

        TestHttp.authenticated(reader)
                .post("/forms/posts/" + post.getId() + "/reactions/like")
                .then()
                .statusCode(200)
                .body(not(containsString("reaction-bar__button--active")));

        long unreadAfterRemove = Given.inject(NotificationRepository.class).countUnread(author.getId());
        assertThat(unreadAfterRemove).isEqualTo(unreadAfterAdd);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("reactionauthor")
                      .withEmail("reactionauthor@test.com")
                      .withName("Reaction Author")
                      .withPassword("password123")
                      .persist();

        reader = Given.user()
                      .withUsername("reactionreader")
                      .withEmail("reactionreader@test.com")
                      .withName("Reaction Reader")
                      .withPassword("password123")
                      .persist();

        post = Given.post()
                    .withAuthor(author)
                    .withTitle("Reactable Post")
                    .withSlug("reactable-post")
                    .withContent("Post body")
                    .withPublished(true)
                    .persist();
    }
}
