package dev.vepo.contraponto.blog;

import dev.vepo.contraponto.renderer.Markdown;
import dev.vepo.contraponto.shared.security.HtmlSanitizer;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
@Unremovable
public class BlogDescriptionRenderer {

    private final HtmlSanitizer htmlSanitizer;

    @Inject
    public BlogDescriptionRenderer(HtmlSanitizer htmlSanitizer) {
        this.htmlSanitizer = htmlSanitizer;
    }

    public String render(String description) {
        if (description == null || description.isBlank()) {
            return "";
        }
        String html = Markdown.getInstance().render(description);
        return htmlSanitizer.sanitizeBlogDescriptionHtml(html);
    }
}
