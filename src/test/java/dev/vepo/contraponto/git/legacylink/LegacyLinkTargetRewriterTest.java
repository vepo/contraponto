package dev.vepo.contraponto.git.legacylink;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class LegacyLinkTargetRewriterTest {

    private static Optional<String> replaceOldDomain(String url) {
        if (url.contains("blog.vepo.dev")) {
            return Optional.of(url.replace("https://blog.vepo.dev", "https://vepo.commit-mestre.dev"));
        }
        return Optional.empty();
    }

    @Test
    void leavesProseMentionOutsideALinkUntouched() {
        String body = "I used to write at https://blog.vepo.dev/posts/hello-world but that's just plain text here.";
        String result = LegacyLinkTargetRewriter.rewrite(body, LegacyLinkTargetRewriterTest::replaceOldDomain);
        assertThat(result).isEqualTo(body);
    }

    @Test
    void leavesUnmatchedLinksUntouched() {
        String body = "[unrelated](https://example.com/other)";
        String result = LegacyLinkTargetRewriter.rewrite(body, LegacyLinkTargetRewriterTest::replaceOldDomain);
        assertThat(result).isEqualTo(body);
    }

    @Test
    void rewritesAsciiDocLinkTarget() {
        String body = "link:https://blog.vepo.dev/posts/hello-world[my old post]";
        String result = LegacyLinkTargetRewriter.rewrite(body, LegacyLinkTargetRewriterTest::replaceOldDomain);
        assertThat(result).isEqualTo("link:https://vepo.commit-mestre.dev/posts/hello-world[my old post]");
    }

    @Test
    void rewritesAutolinkTarget() {
        String body = "Read more at <https://blog.vepo.dev/posts/hello-world>.";
        String result = LegacyLinkTargetRewriter.rewrite(body, LegacyLinkTargetRewriterTest::replaceOldDomain);
        assertThat(result).isEqualTo("Read more at <https://vepo.commit-mestre.dev/posts/hello-world>.");
    }

    @Test
    void rewritesHtmlHrefTarget() {
        String body = "<a href=\"https://blog.vepo.dev/posts/hello-world\">old post</a>";
        String result = LegacyLinkTargetRewriter.rewrite(body, LegacyLinkTargetRewriterTest::replaceOldDomain);
        assertThat(result).isEqualTo("<a href=\"https://vepo.commit-mestre.dev/posts/hello-world\">old post</a>");
    }

    @Test
    void rewritesMarkdownLinkTarget() {
        String body = "See [my old post](https://blog.vepo.dev/posts/hello-world) for details.";
        String result = LegacyLinkTargetRewriter.rewrite(body, LegacyLinkTargetRewriterTest::replaceOldDomain);
        assertThat(result).isEqualTo("See [my old post](https://vepo.commit-mestre.dev/posts/hello-world) for details.");
    }
}
