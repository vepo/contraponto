package dev.vepo.contraponto.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class AuthorSocialUrlsTest {

    @Test
    void sameAsIncludesBluesky() {
        var user = new User();
        user.setBlueskyUrl("https://bsky.app/profile/example.bsky.social");

        assertThat(AuthorSocialUrls.sameAs(user)).contains("https://bsky.app/profile/example.bsky.social");
    }

    @Test
    void visibleLinksIncludesBluesky() {
        var user = new User();
        user.setBlueskyUrl("https://bsky.app/profile/example.bsky.social");

        var links = AuthorSocialUrls.visibleLinks(user);

        assertThat(links).anyMatch(link -> "bluesky".equals(link.key()) && link.url().contains("bsky.app"));
    }
}
