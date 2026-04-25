package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import dev.vepo.contraponto.post.Post;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TemplateExtensions {

    @TemplateExtension
    public static String escapeHtml(String value) {
        if (Objects.nonNull(value)) {
            return value.replaceAll("&", "&amp;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;")
                        .replaceAll("\"", "&quot;")
                        .replaceAll("'", "&#39;");
        } else {
            return "null";
        }
    }

    @TemplateExtension
    public static String avatarUrl(LoggedUser user) {
        if (Objects.nonNull(user)) {
            return "";
        } else {
            return "";
        }
    }

    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();

    @TemplateExtension
    public static String markdown2Html(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        return renderer.render(parser.parse(content));
    }

    @TemplateExtension
    public static String coverUrl(Post post) {
        if (Objects.nonNull(post) && Objects.nonNull(post.getCover())) {
            return post.getCover().getUrl();
        } else {
            return null;
        }
    }

    @TemplateExtension
    public static String readTime(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "0 min read";
        }

        String[] words = content.trim().split("\\s+");
        int wordCount = words.length;
        int wordsPerMinute = 200; // average reading speed
        int minutes = (int) Math.ceil((double) wordCount / wordsPerMinute);

        if (minutes < 1) {
            return "< 1 min read";
        }
        return minutes + " min read";
    }

    @TemplateExtension
    public static String firstName(LoggedUser user) {
        if (user == null) {
            return "";
        }
        return user.getFirstName();
    }

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @TemplateExtension
    public static String formatDate(LocalDateTime date) {
        return date.format(formatter);
    }
}