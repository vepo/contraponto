package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.user.User;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostPublicationServiceLegacyLinkTest {

    private static String replaceLegacy(String content) {
        return content == null ? null : content.replace("OLD-LINK", "NEW-LINK");
    }

    @Inject
    PostPublicationService postPublicationService;

    @Inject
    PostPublicationRepository publicationRepository;

    @Test
    void isIdempotentWhenAppliedTwice() {
        User author = Given.user()
                           .withUsername("legacyauthor3")
                           .withEmail("legacyauthor3@example.com")
                           .withPassword("pw123456789")
                           .withName("Legacy Author Three")
                           .persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withSlug("idempotent-post")
                        .withContent("[old](OLD-LINK)")
                        .withPublished(true)
                        .persist();

        postPublicationService.reprocessLegacyLinks(post, PostPublicationServiceLegacyLinkTest::replaceLegacy);
        postPublicationService.reprocessLegacyLinks(post, PostPublicationServiceLegacyLinkTest::replaceLegacy);

        assertThat(post.getContent()).isEqualTo("[old](NEW-LINK)");
        Optional<Integer> maxVersion = publicationRepository.findMaxVersion(post.getId());
        assertThat(maxVersion).contains(1);
    }

    @Test
    void leavesContentUnchangedWhenNothingMatches() {
        User author = Given.user()
                           .withUsername("legacyauthor2")
                           .withEmail("legacyauthor2@example.com")
                           .withPassword("pw123456789")
                           .withName("Legacy Author Two")
                           .persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withSlug("no-match-post")
                        .withContent("nothing to rewrite here")
                        .withPublished(true)
                        .persist();

        postPublicationService.reprocessLegacyLinks(post, PostPublicationServiceLegacyLinkTest::replaceLegacy);

        assertThat(post.getContent()).isEqualTo("nothing to rewrite here");
    }

    @Test
    void rewritesWorkingCopyAndEverySnapshotWithoutCreatingNewVersion() {
        User author = Given.user()
                           .withUsername("legacyauthor1")
                           .withEmail("legacyauthor1@example.com")
                           .withPassword("pw123456789")
                           .withName("Legacy Author One")
                           .persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withSlug("legacy-post")
                        .withContent("first [old](OLD-LINK)")
                        .withPublished(true)
                        .persist();

        post.setContent("second [old](OLD-LINK)");
        postPublicationService.publish(post);
        int versionsBefore = Given.transaction(() -> publicationRepository.findByPostIdOrderByVersionDesc(post.getId())).size();
        assertThat(versionsBefore).isEqualTo(2);

        postPublicationService.reprocessLegacyLinks(post, PostPublicationServiceLegacyLinkTest::replaceLegacy);

        assertThat(post.getContent()).contains("NEW-LINK").doesNotContain("OLD-LINK");
        List<PostPublication> snapshots = Given.transaction(() -> publicationRepository.findByPostIdOrderByVersionDesc(post.getId()));
        assertThat(snapshots).hasSize(versionsBefore);
        assertThat(snapshots).allSatisfy(snapshot -> {
            assertThat(snapshot.getContent()).contains("NEW-LINK").doesNotContain("OLD-LINK");
            assertThat(snapshot.getRenderedHtml()).contains("NEW-LINK");
        });
    }

    @BeforeEach
    void wipeData() {
        Given.cleanup();
    }
}
