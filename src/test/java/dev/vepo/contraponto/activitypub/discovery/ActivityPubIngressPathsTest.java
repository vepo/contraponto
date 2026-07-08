package dev.vepo.contraponto.activitypub.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class ActivityPubIngressPathsTest {

    @Test
    void matches_returnsFalseForBlogPaths() {
        assertThat(ActivityPubIngressPaths.matches("/vepo/post/hello")).isFalse();
        assertThat(ActivityPubIngressPaths.matches("/vepo/notas")).isFalse();
    }

    @Test
    void matches_returnsTrueForProtocolPaths() {
        assertThat(ActivityPubIngressPaths.matches("/vepo/inbox")).isTrue();
        assertThat(ActivityPubIngressPaths.matches("/api/nodeinfo")).isTrue();
        assertThat(ActivityPubIngressPaths.matches("/.well-known/webfinger")).isTrue();
    }

    @Test
    void resolveInternalPath_ignoresBlogPaths() {
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo")).isEmpty();
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/post/hello")).isEmpty();
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/notas")).isEmpty();
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/api/images/uuid")).isEmpty();
    }

    @Test
    void resolveInternalPath_rejectsReservedUsernameSegments() {
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/api/inbox")).isEmpty();
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/forms/inbox")).isEmpty();
    }

    @Test
    void resolveInternalPath_rewritesNodeInfoPaths() {
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/nodeinfo/2.0")).contains("/__activity_pub__/nodeinfo/2.0");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/nodeinfo/2.1")).contains("/__activity_pub__/nodeinfo/2.1");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/api/nodeinfo")).contains("/__activity_pub__/nodeinfo/2.0");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/api/nodeinfo")).contains("/__activity_pub__/nodeinfo/2.0");
    }

    @Test
    void resolveInternalPath_rewritesUserCollectionPaths() {
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/inbox")).contains("/__activity_pub__/user/vepo/inbox");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/outbox")).contains("/__activity_pub__/user/vepo/outbox");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/followers")).contains("/__activity_pub__/user/vepo/followers");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/following")).contains("/__activity_pub__/user/vepo/following");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/poco")).contains("/__activity_pub__/user/vepo/poco");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/vepo/activities/create/42")).contains("/__activity_pub__/user/vepo/activities/create/42");
    }

    @Test
    void resolveInternalPath_rewritesWellKnownPaths() {
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/.well-known/webfinger")).contains("/__activity_pub__/well-known/webfinger");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/.well-known/host-meta")).contains("/__activity_pub__/well-known/host-meta");
        assertThat(ActivityPubIngressPaths.resolveInternalPath("/.well-known/nodeinfo")).contains("/__activity_pub__/well-known/nodeinfo");
    }
}
