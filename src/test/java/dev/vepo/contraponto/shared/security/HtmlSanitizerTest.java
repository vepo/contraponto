package dev.vepo.contraponto.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class HtmlSanitizerTest {

    @Inject
    HtmlSanitizer htmlSanitizer;

    @Test
    void blogDescriptionAllowsBoldNotIframe() {
        String input = "<p><strong>Bold</strong></p><iframe src=\"https://www.youtube.com/embed/x\"></iframe>";
        String cleaned = htmlSanitizer.sanitizeBlogDescriptionHtml(input);
        assertThat(cleaned).contains("<strong>Bold</strong>").doesNotContain("iframe");
    }

    @Test
    void keepsGistScriptSrc() {
        String input = "<script src=\"https://gist.github.com/vepo/abc123.js\"></script>";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).contains("gist.github.com/vepo/abc123.js");
    }

    @Test
    void keepsSafeMarkup() {
        String input = "<h1>Title</h1><p>Text with <strong>bold</strong></p>";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).contains("<h1>", "<strong>");
    }

    @Test
    void keepsYoutubeEmbedIframe() {
        String input =
                """
                <iframe width="560" height="315" src="https://www.youtube.com/embed/hPoHp0WhglA" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin" allowfullscreen></iframe>
                """;
        assertThat(htmlSanitizer.sanitizePostHtml(input)).contains("youtube.com/embed/hPoHp0WhglA");
    }

    @Test
    void preservesApiImageSrc() {
        String input = "<p><img src=\"/api/images/550e8400-e29b-41d4-a716-446655440000.png\" alt=\"caption\"></p>";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).contains("src=\"/api/images/550e8400-e29b-41d4-a716-446655440000.png\"");
    }

    @Test
    void removesOnerrorHandlers() {
        String input = "<img src=\"/x.png\" onerror=\"alert(1)\" alt=\"x\">";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).doesNotContain("onerror");
    }

    @Test
    void stripsArbitraryInlineScript() {
        String input = "<script>alert(1)</script><script src=\"https://evil.example/x.js\"></script>";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).isEmpty();
    }

    @Test
    void stripsJavascriptImageSrc() {
        String input = "<img src=\"javascript:alert(1)\" alt=\"x\">";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).doesNotContain("javascript:");
    }

    @Test
    void stripsScriptTags() {
        String input = "<p>Hello</p><script>alert(1)</script>";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).isEqualTo("<p>Hello</p>");
    }
}
