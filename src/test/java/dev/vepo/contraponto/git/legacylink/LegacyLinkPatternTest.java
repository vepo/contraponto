package dev.vepo.contraponto.git.legacylink;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LegacyLinkPatternTest {

    @Test
    void doesNotMatchDifferentPath() {
        var pattern = LegacyLinkPattern.parse("blog.vepo.dev/posts/{slug}").orElseThrow();

        assertThat(pattern.matches("https://blog.vepo.dev/other/hello-world")).isFalse();
        assertThat(pattern.matches("https://other.example.com/posts/hello-world")).isFalse();
    }

    @Test
    void extractsSlugIgnoringSchemeAndTrailingSlash() {
        var pattern = LegacyLinkPattern.parse("blog.vepo.dev/posts/{slug}").orElseThrow();

        assertThat(pattern.extractSlug("https://blog.vepo.dev/posts/hello-world")).contains("hello-world");
        assertThat(pattern.extractSlug("http://blog.vepo.dev/posts/hello-world/")).contains("hello-world");
        assertThat(pattern.extractSlug("blog.vepo.dev/posts/hello-world")).contains("hello-world");
    }

    @Test
    void rejectsBlankPattern() {
        assertThat(LegacyLinkPattern.parse("   ")).isEmpty();
        assertThat(LegacyLinkPattern.parse(null)).isEmpty();
    }

    @Test
    void rejectsPatternWithMultipleSlugTokens() {
        assertThat(LegacyLinkPattern.parse("blog.vepo.dev/{slug}/posts/{slug}")).isEmpty();
    }

    @Test
    void rejectsPatternWithoutHost() {
        assertThat(LegacyLinkPattern.parse("{slug}")).isEmpty();
    }

    @Test
    void rejectsPatternWithoutSlugToken() {
        assertThat(LegacyLinkPattern.parse("blog.vepo.dev/posts/")).isEmpty();
    }

    @Test
    void rejectsSlugSpanningMultipleSegments() {
        var pattern = LegacyLinkPattern.parse("blog.vepo.dev/posts/{slug}").orElseThrow();

        assertThat(pattern.extractSlug("https://blog.vepo.dev/posts/hello/world")).isEmpty();
    }
}
