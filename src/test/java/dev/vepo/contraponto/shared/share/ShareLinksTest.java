package dev.vepo.contraponto.shared.share;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class ShareLinksTest {

    @Test
    void fromBuildsShareTextAndIntentUrls() {
        var share = ShareLinks.from("My Post Title", "https://example.com/alice/post/my-slug");

        assertThat(share.shareText()).isEqualTo("My Post Title https://example.com/alice/post/my-slug");
        assertThat(share.linkedInUrl()).isEqualTo(
                                                  "https://www.linkedin.com/sharing/share-offsite/?url=https%3A%2F%2Fexample.com%2Falice%2Fpost%2Fmy-slug");
        assertThat(share.blueskyUrl()).startsWith("https://bsky.app/intent/compose?text=");
        assertThat(share.blueskyUrl()).contains("My+Post+Title");
        assertThat(share.blueskyUrl()).contains("https%3A%2F%2Fexample.com%2Falice%2Fpost%2Fmy-slug");
    }

    @Test
    void fromTrimsTitleAndUrl() {
        var share = ShareLinks.from("  Blog Name  ", "  https://example.com/alice  ");

        assertThat(share.shareText()).isEqualTo("Blog Name https://example.com/alice");
    }
}
