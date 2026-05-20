package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostPublicationServiceTest {

    @Inject
    PostPublicationService publicationService;

    @Inject
    PostPublicationRepository publicationRepository;

    @org.junit.jupiter.api.BeforeEach
    void clean() {
        Given.cleanup();
    }

    @Test
    void publish_creates_version_and_draft_save_does_not_change_live() {
        User author = Given.user()
                           .withUsername("pubauthor1")
                           .withEmail("pubauthor1@example.com")
                           .withPassword("pw123456789")
                           .withName("Pub Author")
                           .persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Live title")
                        .withContent("Version one")
                        .withSlug("history-test")
                        .withPublished(true)
                        .persist();

        assertThat(post.getLivePublication()).isNotNull();
        assertThat(post.getLivePublication().getVersion()).isEqualTo(1);
        assertThat(post.getLivePublication().getContent()).isEqualTo("Version one");

        post.setContent("Draft only change");
        assertThat(publicationService.hasUnpublishedChanges(post)).isTrue();

        var versions = publicationRepository.findByPostIdOrderByVersionDesc(post.getId());
        assertThat(versions).hasSize(1);
        assertThat(versions.getFirst().getContent()).isEqualTo("Version one");
    }

    @Test
    void publish_truncates_long_description_on_snapshot() {
        User author = Given.user()
                           .withUsername("pubauthor3")
                           .withEmail("pubauthor3@example.com")
                           .withPassword("pw123456789")
                           .withName("Pub Author Three")
                           .persist();
        String longDescription = "z".repeat(600);
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Long desc")
                        .withDescription(longDescription)
                        .withContent("Body")
                        .withSlug("long-desc-test")
                        .withPublished(false)
                        .persist();

        publicationService.publish(post);

        assertThat(post.getDescription()).hasSize(PostPublicationDescriptions.MAX_LENGTH);
        assertThat(post.getLivePublication().getDescription()).isEqualTo(post.getDescription());
        assertThat(publicationService.hasUnpublishedChanges(post)).isFalse();
    }

    @Test
    void second_publish_creates_version_two_and_skips_identical_republish() {
        User author = Given.user()
                           .withUsername("pubauthor2")
                           .withEmail("pubauthor2@example.com")
                           .withPassword("pw123456789")
                           .withName("Pub Author Two")
                           .persist();
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Title")
                        .withContent("Body v1")
                        .withSlug("republish-test")
                        .withPublished(false)
                        .persist();

        publicationService.publish(post);
        assertThat(publicationRepository.findMaxVersion(post.getId())).contains(1);

        post.setContent("Body v2");
        publicationService.publish(post);
        assertThat(publicationRepository.findMaxVersion(post.getId())).contains(2);

        int versionBefore = post.getLivePublication().getVersion();
        publicationService.publish(post);
        assertThat(post.getLivePublication().getVersion()).isEqualTo(versionBefore);
        assertThat(publicationRepository.findByPostIdOrderByVersionDesc(post.getId())).hasSize(2);
    }
}
