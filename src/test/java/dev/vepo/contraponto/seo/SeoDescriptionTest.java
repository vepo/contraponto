package dev.vepo.contraponto.seo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;

@QuarkusIntegrationTest
class SeoDescriptionTest {

    @Test
    void stripsMarkdownAndTruncates() {
        String markdown = "## Title\n\nHello **world** with [link](https://example.com).";
        String plain = SeoDescription.toPlainText(markdown);
        assertThat(plain).doesNotContain("**").doesNotContain("##");
        assertThat(plain.length()).isLessThanOrEqualTo(160);
    }
}
