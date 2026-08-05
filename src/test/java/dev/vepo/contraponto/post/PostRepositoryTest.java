package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostRepositoryTest {

    @Inject
    PostRepository postRepository;

    @Test
    void findByAuthorAndSlugDoesNotResolveAnotherAuthorsMatchingSlug() {
        User author = Given.user()
                           .withUsername("xblogauthor2")
                           .withEmail("xblogauthor2@example.com")
                           .withPassword("pw123456789")
                           .withName("Cross Blog Author Two")
                           .persist();
        User other = Given.user()
                          .withUsername("otherauthor")
                          .withEmail("otherauthor@example.com")
                          .withPassword("pw123456789")
                          .withName("Other Author")
                          .persist();
        Given.post()
             .withAuthor(other)
             .withSlug("only-others-slug")
             .withContent("Body")
             .withPublished(true)
             .persist();

        var found = postRepository.findByAuthorAndSlug(author.getId(), "only-others-slug");

        assertThat(found).isEmpty();
    }

    @Test
    void findByAuthorAndSlugResolvesAcrossBlogsForSameAuthor() {
        User author = Given.user()
                           .withUsername("crossblogauthor")
                           .withEmail("crossblogauthor@example.com")
                           .withPassword("pw123456789")
                           .withName("Cross Blog Author")
                           .persist();
        var secondaryBlog = Given.blog().withUser(author).withSlug("secondary").persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(secondaryBlog)
                        .withSlug("shared-slug")
                        .withContent("Body")
                        .withPublished(true)
                        .persist();

        var found = postRepository.findByAuthorAndSlug(author.getId(), "shared-slug");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(post.getId());
    }

    @BeforeEach
    void wipeData() {
        Given.cleanup();
    }
}
