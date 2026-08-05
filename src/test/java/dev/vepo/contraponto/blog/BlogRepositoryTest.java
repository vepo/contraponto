package dev.vepo.contraponto.blog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusIntegrationTest
class BlogRepositoryTest {

    @Inject
    BlogRepository blogRepository;

    @Inject
    EntityManager entityManager;

    private User owner;

    @Test
    void dbRejectsDuplicateRemoteAndConfigPathForSameOwner() {
        Throwable thrown = catchThrowable(() -> Given.transaction(() -> {
            var duplicate = new Blog(entityManager.find(dev.vepo.contraponto.user.User.class, owner.getId()),
                                     "second",
                                     "Second",
                                     "Second");
            duplicate.setGitRemoteUrl("https://github.com/vepo/contraponto.git");
            duplicate.setGitConfigPath("main/_contraponto.yml");
            entityManager.persist(duplicate);
            entityManager.flush();
        }));
        assertThat(thrown).isNotNull();
    }

    @Test
    void existsGitRemoteAndConfigPathExcludesSelfOnUpdate() {
        boolean exists = blogRepository.existsGitRemoteAndConfigPath(owner.getId(),
                                                                     "https://github.com/vepo/contraponto.git",
                                                                     "main/_contraponto.yml",
                                                                     owner.getDefaultBlog().getId());
        assertThat(exists).isFalse();
    }

    @Test
    void existsGitRemoteAndConfigPathMatchesExplicitPath() {
        boolean exists = blogRepository.existsGitRemoteAndConfigPath(owner.getId(),
                                                                     "https://github.com/vepo/contraponto.git",
                                                                     "main/_contraponto.yml",
                                                                     null);
        assertThat(exists).isTrue();
    }

    @Test
    void existsGitRemoteAndConfigPathTreatsBlankAsDistinctFromExplicitPath() {
        boolean exists = blogRepository.existsGitRemoteAndConfigPath(owner.getId(),
                                                                     "https://github.com/vepo/contraponto.git",
                                                                     null,
                                                                     null);
        assertThat(exists).isFalse();
    }

    @BeforeEach
    void setup() {
        Given.cleanup();
        owner = Given.user()
                     .withUsername("blogrepoowner")
                     .withEmail("blogrepoowner@example.com")
                     .withPassword("Password123!")
                     .withName("Blog Repo Owner")
                     .persist();
        Given.transaction(() -> {
            var managed = entityManager.find(Blog.class, owner.getDefaultBlog().getId());
            managed.setGitRemoteUrl("https://github.com/vepo/contraponto.git");
            managed.setGitConfigPath("main/_contraponto.yml");
        });
    }
}
