package dev.vepo.contraponto.git;

import com.fasterxml.jackson.core.JsonProcessingException;

import dev.vepo.contraponto.post.PostPublicationDescriptions;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GitImportFailureMapper {

    public record ClassifiedImportFailure(String message, String remediation) {}

    private static final String LAYOUT_CONVENTION_DOC = "See docs/git-jekyll-convention.md.";

    private static String firstLine(String message) {
        if (message == null) {
            return null;
        }
        int nl = message.indexOf('\n');
        return nl >= 0 ? message.substring(0, nl).strip() : message.strip();
    }

    private static ClassifiedImportFailure genericFailure(String detail) {
        return new ClassifiedImportFailure(
                                           "Could not import this post from Git.",
                                           detail + " " + LAYOUT_CONVENTION_DOC);
    }

    private static boolean isDescriptionTooLong(String text) {
        return text.contains("value too long") || text.contains("character varying(512)")
                || (text.contains("512") && text.contains("description"));
    }

    private static boolean isSlugConflict(String text) {
        return text.contains("uk_posts_slug") || text.contains("duplicate key")
                && (text.contains("slug") || text.contains("tb_posts"));
    }

    private static boolean isYamlParseFailure(Throwable root, String text) {
        if (root instanceof JsonProcessingException) {
            return true;
        }
        return text.contains("while parsing") || text.contains("yaml") && text.contains("parse")
                || text.contains("unexpected character") && text.contains("---");
    }

    private static String messageChain(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(current.getMessage());
            }
            Throwable cause = current.getCause();
            if (cause == null || cause == current) {
                break;
            }
            current = cause;
        }
        return sb.toString();
    }

    private static Throwable rootCause(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    public ClassifiedImportFailure classify(Throwable throwable) {
        if (throwable == null) {
            return genericFailure("Unknown error");
        }
        Throwable root = rootCause(throwable);
        String text = messageChain(throwable).toLowerCase();

        if (isYamlParseFailure(root, text)) {
            return new ClassifiedImportFailure(
                                               "Invalid YAML front matter.",
                                               "Fix the block between the opening and closing --- lines. " + LAYOUT_CONVENTION_DOC);
        }
        if (isDescriptionTooLong(text)) {
            return new ClassifiedImportFailure(
                                               "Description too long for a published snapshot.",
                                               "Published excerpts are limited to " + PostPublicationDescriptions.MAX_LENGTH
                                                       + " characters; shorten the description in Git or in Contraponto.");
        }
        if (isSlugConflict(text)) {
            return new ClassifiedImportFailure(
                                               "Slug already used on this blog.",
                                               "Use a different slug or permalink, or set contraponto_post_id to the existing post id. "
                                                       + LAYOUT_CONVENTION_DOC);
        }
        String rootLine = firstLine(root.getMessage());
        if (rootLine != null && !rootLine.isBlank()) {
            return genericFailure(rootLine);
        }
        return genericFailure(root.getClass().getSimpleName());
    }
}
