package dev.vepo.contraponto.shared.infra;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.renderer.Renderer;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TemplateExtensions {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<String, String> HTML_ENTITIES = Map.of("&", "&amp;",
                                                                    "<", "&lt;",
                                                                    ">", "&gt;",
                                                                    "\"", "&quot;",
                                                                    "'", "&#39;");

    private static final Pattern HTML_SPECIAL_CHARS = Pattern.compile(HTML_ENTITIES.keySet()
                                                                                   .stream()
                                                                                   .map(Pattern::quote)
                                                                                   .collect(Collectors.joining("|")));

    @TemplateExtension
    public static String avatarUrl(LoggedUser user) {
        if (Objects.nonNull(user)) {
            return "https://ui-avatars.com/api/?name=%s&background=1a8917&color=fff&bold=true&length=2".formatted(URLEncoder.encode(user.getName(),
                                                                                                                                    StandardCharsets.UTF_8));
        } else {
            return "";
        }
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
    public static String escapeHtml(String value) {
        if (Objects.nonNull(value)) {
            return HTML_SPECIAL_CHARS.matcher(value)
                                     .replaceAll(m -> HTML_ENTITIES.get(m.group()));
        } else {
            return "null";
        }
    }

    @TemplateExtension
    public static String firstName(LoggedUser user) {
        if (user == null) {
            return "";
        }
        return user.getFirstName();
    }

    @TemplateExtension
    public static String formatDate(LocalDateTime date) {
        return date.format(formatter);
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
    public static String render(Post post) {
        if (post == null || post.getContent() == null || post.getContent().trim().isEmpty()) {
            return "";
        }

        return Renderer.get(post.getFormat())
                       .render(post.getContent());
    }

    private TemplateExtensions() {
        /* This utility class should not be instantiated */
    }
}