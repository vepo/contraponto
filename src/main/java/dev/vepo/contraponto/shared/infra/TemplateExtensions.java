package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TemplateExtensions {

    // @TemplateExtension
    // public static String or(UserContext.UserInfo user, Object defaultValue) {
    // return user != null ? user.getName() : String.valueOf(defaultValue);
    // }

    // @TemplateExtension
    // public static boolean isAuthenticated(UserContext.UserInfo user) {
    // return user != null && user.isAuthenticated();
    // }

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

    // @TemplateExtension
    // public static String initials(UserContext.UserInfo user) {
    // if (user == null)
    // return "";
    // return user.getInitials();
    // }

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