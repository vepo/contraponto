package dev.vepo.contraponto.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@QuarkusTest
class BlogAudienceServiceTest {

    @Inject
    BlogAudienceService blogAudienceService;

    @Inject
    BlogAudienceRepository audienceRepository;

    @Inject
    EntityManager entityManager;

    private User author;
    private User follower;
    private Blog authorBlog;

    @Test
    void isFollowingReturnsFalseWhenNoRow() {
        assertThat(blogAudienceService.isFollowing(follower.getId(), authorBlog.getId())).isFalse();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("audauth")
                      .withEmail("audauth@example.com")
                      .withPassword("Password123!")
                      .withName("Audience Author")
                      .persist();
        follower = Given.user()
                        .withUsername("audfollow")
                        .withEmail("audfollow@example.com")
                        .withPassword("Password123!")
                        .withName("Audience Follower")
                        .persist();
        authorBlog = author.getDefaultBlog();
    }

    @Test
    void toggleFollowOnOwnBlogThrowsBadRequest() {
        assertThatThrownBy(() -> blogAudienceService.toggleFollow(author.getId(), authorBlog.getId()))
                                                                                                      .isInstanceOf(BadRequestException.class)
                                                                                                      .hasMessageContaining("own blog");
    }

    @Test
    void toggleOnInactiveBlogThrowsNotFound() {
        Given.transaction(() -> entityManager.find(Blog.class, authorBlog.getId()).setActive(false));

        assertThatThrownBy(() -> blogAudienceService.toggleFollow(follower.getId(), authorBlog.getId()))
                                                                                                        .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unsubscribeDeletesInactiveRow() {
        assertThat(blogAudienceService.toggleFollow(follower.getId(), authorBlog.getId())).isTrue();
        assertThat(blogAudienceService.isFollowing(follower.getId(), authorBlog.getId())).isTrue();

        assertThat(blogAudienceService.toggleFollow(follower.getId(), authorBlog.getId())).isFalse();
        assertThat(blogAudienceService.isFollowing(follower.getId(), authorBlog.getId())).isFalse();
        assertThat(audienceRepository.findByUserAndBlog(follower.getId(), authorBlog.getId())).isEmpty();
    }
}
