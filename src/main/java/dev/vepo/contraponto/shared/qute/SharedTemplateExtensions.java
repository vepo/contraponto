package dev.vepo.contraponto.shared.qute;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class SharedTemplateExtensions {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
    public static String escapeHtml(String value) {
        if (Objects.nonNull(value)) {
            return HTML_SPECIAL_CHARS.matcher(value)
                                     .replaceAll(m -> HTML_ENTITIES.get(m.group()));
        }
        return "null";
    }

    @TemplateExtension
    public static String formatDate(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        return date.format(FORMATTER);
    }

    @TemplateExtension
    public static String formatReadingDuration(long seconds) {
        if (seconds <= 0) {
            return "< 1 min";
        }
        if (seconds < 60) {
            return "< 1 min";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return "%s min".formatted(minutes);
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return "%sh".formatted(hours);
        }
        return "%sh %sm".formatted(hours, remainingMinutes);
    }

    @TemplateExtension
    public static String formatReadingTimeTotal(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0 min";
        }
        long minutes = totalSeconds / 60;
        if (minutes < 60) {
            return "%s min".formatted(minutes);
        }
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return "%sh".formatted(hours);
        }
        return "%sh %sm".formatted(hours, remainingMinutes);
    }

    @TemplateExtension
    public static String readTime(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "0 min read";
        }

        String[] words = content.trim().split("\\s+");
        int wordCount = words.length;
        int wordsPerMinute = 200;
        int minutes = (int) Math.ceil((double) wordCount / wordsPerMinute);

        if (minutes < 1) {
            return "< 1 min read";
        }
        return "%s min read".formatted(minutes);
    }

    private SharedTemplateExtensions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
