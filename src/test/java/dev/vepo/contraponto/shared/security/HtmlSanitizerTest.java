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
    void keepsSafeMarkup() {
        String input = "<h1>Title</h1><p>Text with <strong>bold</strong></p>";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).contains("<h1>", "<strong>");
    }

    @Test
    void removesOnerrorHandlers() {
        String input = "<img src=\"/x.png\" onerror=\"alert(1)\" alt=\"x\">";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).doesNotContain("onerror");
    }

    @Test
    void stripsScriptTags() {
        String input = "<p>Hello</p><script>alert(1)</script>";
        assertThat(htmlSanitizer.sanitizePostHtml(input)).isEqualTo("<p>Hello</p>");
    }
}
