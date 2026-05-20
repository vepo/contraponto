package dev.vepo.contraponto.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class HtmlSanitizerTwitterEmbedTest {

    @Inject
    HtmlSanitizer htmlSanitizer;

    @Test
    void keepsTwitterWidgetsScript() {
        String html = """
                      <div class="content-render content-render--twitter">
                      <blockquote class="twitter-tweet"><p>hello</p></blockquote>
                      <script async src="https://platform.twitter.com/widgets.js" charset="utf-8"></script>
                      </div>
                      """;
        assertThat(htmlSanitizer.sanitizePostHtml(html)).contains("platform.twitter.com/widgets.js");
    }
}
