package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

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

    @Test
    void alignPublicationTimestampFromGitUpdatesLiveSnapshotWhenDatesDiffer() {
        User author = Given.user()
                           .withUsername("pubauthor6")
                           .withEmail("pubauthor6@example.com")
                           .withPassword("pw123456789")
                           .withName("Pub Author Six")
                           .persist();
        LocalDateTime originalPublishedAt = LocalDateTime.of(2019, 6, 12, 8, 30);
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Title")
                        .withContent("Body")
                        .withSlug("align-date-test")
                        .withPublished(true)
                        .withPublishedAt(originalPublishedAt)
                        .persist();

        LocalDateTime gitPublishedAt = LocalDateTime.of(2020, 2, 1, 14, 0);
        post.setPublishedAt(gitPublishedAt);
        publicationService.alignPublicationTimestampFromGit(post);

        assertThat(post.getLivePublication().getPublishedAt()).isEqualTo(gitPublishedAt);
    }

    @org.junit.jupiter.api.BeforeEach
    void clean() {
        Given.cleanup();
    }

    @Test
    void first_publish_uses_post_published_at_when_already_set() {
        User author = Given.user()
                           .withUsername("pubauthor4")
                           .withEmail("pubauthor4@example.com")
                           .withPassword("pw123456789")
                           .withName("Pub Author Four")
                           .persist();
        LocalDateTime gitPublishedAt = LocalDateTime.of(2019, 6, 12, 8, 30);
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Imported")
                        .withContent("Body")
                        .withSlug("git-date-test")
                        .withPublished(false)
                        .withPublishedAt(gitPublishedAt)
                        .persist();

        publicationService.publish(post);

        assertThat(post.getPublishedAt()).isEqualTo(gitPublishedAt);
        assertThat(post.getLivePublication().getPublishedAt()).isEqualTo(gitPublishedAt);
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
    void republish_uses_current_time_even_when_post_published_at_is_older() {
        User author = Given.user()
                           .withUsername("pubauthor5")
                           .withEmail("pubauthor5@example.com")
                           .withPassword("pw123456789")
                           .withName("Pub Author Five")
                           .persist();
        LocalDateTime originalPublishedAt = LocalDateTime.of(2019, 6, 12, 8, 30);
        var post = Given.post()
                        .withAuthor(author)
                        .withTitle("Title")
                        .withContent("Body v1")
                        .withSlug("republish-date-test")
                        .withPublished(false)
                        .withPublishedAt(originalPublishedAt)
                        .persist();

        publicationService.publish(post);
        assertThat(post.getLivePublication().getPublishedAt()).isEqualTo(originalPublishedAt);

        post.setContent("Body v2");
        publicationService.publish(post);

        assertThat(post.getPublishedAt()).isEqualTo(originalPublishedAt);
        assertThat(post.getLivePublication().getVersion()).isEqualTo(2);
        assertThat(post.getLivePublication().getPublishedAt()).isAfter(originalPublishedAt);
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
