package dev.vepo.contraponto.git;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import dev.vepo.contraponto.renderer.Format;

/**
 * Resolves Jekyll / legacy front matter aliases on Git import.
 * Contraponto-native keys take precedence when both are present.
 */
final class GitFrontMatterResolver {

    private static final DateTimeFormatter JEKYLL_PUBLISH_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT);

    static String assetBasename(String assetRelativePath) {
        int slash = assetRelativePath.lastIndexOf('/');
        return slash >= 0 ? assetRelativePath.substring(slash + 1) : assetRelativePath;
    }

    private static Format formatFromExtension(Path postFile) {
        Path fname = postFile.getFileName();
        if (fname == null) {
            return Format.MARKDOWN;
        }
        String n = fname.toString().toLowerCase(Locale.ROOT);
        if (n.endsWith(".adoc") || n.endsWith(".asciidoc")) {
            return Format.ASCIIDOC;
        }
        return Format.MARKDOWN;
    }

    private static Optional<Boolean> parseBoolean(Object raw) {
        if (raw == null) {
            return Optional.empty();
        }
        if (raw instanceof Boolean b) {
            return Optional.of(b);
        }
        String s = raw.toString().strip().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "true", "yes", "on" -> Optional.of(true);
            case "false", "no", "off" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    static LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(raw).toLocalDateTime();
        } catch (DateTimeParseException _) {
            try {
                return OffsetDateTime.parse(raw, JEKYLL_PUBLISH_DATE).toLocalDateTime();
            } catch (DateTimeParseException _) {
                try {
                    return LocalDateTime.parse(raw);
                } catch (DateTimeParseException _) {
                    try {
                        LocalDate dateOnly = LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE);
                        return dateOnly.atStartOfDay();
                    } catch (DateTimeParseException _) {
                        return null;
                    }
                }
            }
        }
    }

    private static Format parseFormatEnum(String fm) {
        if (fm == null || fm.isBlank()) {
            return null;
        }
        try {
            return Format.valueOf(fm.strip().replace(' ', '_').toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    static String resolveCoverPath(Map<String, Object> frontMatter) {
        String cover = trimToNull(frontMatter.get("cover"));
        if (cover != null) {
            return cover;
        }
        return trimToNull(frontMatter.get("image"));
    }

    static Format resolveFormat(Map<String, Object> frontMatter, Path postFile) {
        Format fromFm = parseFormatEnum(trimToNull(frontMatter.get("format")));
        if (fromFm != null) {
            return fromFm;
        }
        return formatFromExtension(postFile);
    }

    static boolean resolvePublished(Map<String, Object> frontMatter, boolean folderDefaultPublished) {
        return parseBoolean(frontMatter.get("published")).orElse(folderDefaultPublished);
    }

    static LocalDateTime resolvePublishedAt(Map<String, Object> frontMatter, Optional<LocalDate> filenameDate) {
        LocalDateTime fromFm = parseDateTime(trimToNull(frontMatter.get("published_at")));
        if (fromFm != null) {
            return fromFm;
        }
        fromFm = parseDateTime(trimToNull(frontMatter.get("publish_date")));
        if (fromFm != null) {
            return fromFm;
        }
        return filenameDate.map(LocalDate::atStartOfDay).orElse(null);
    }

    static String resolveSerieTitle(Map<String, Object> frontMatter) {
        String serie = trimToNull(frontMatter.get("serie"));
        if (serie != null) {
            return serie;
        }
        return trimToNull(frontMatter.get("series"));
    }

    static String resolveSlug(Map<String, Object> frontMatter, String filenameStem) {
        String explicit = trimToNull(frontMatter.get("slug"));
        if (explicit != null) {
            return explicit.toLowerCase(Locale.ROOT);
        }
        String fromPermalink = slugFromPermalink(trimToNull(frontMatter.get("permalink")));
        if (fromPermalink != null) {
            return fromPermalink;
        }
        if (filenameStem == null || filenameStem.isBlank()) {
            return "";
        }
        return filenameStem.toLowerCase(Locale.ROOT);
    }

    static String slugFromPermalink(String permalink) {
        if (permalink == null || permalink.isBlank()) {
            return null;
        }
        String path = permalink.strip();
        if (path.contains("://")) {
            int schemeEnd = path.indexOf("://");
            int pathStart = path.indexOf('/', schemeEnd + 3);
            path = pathStart >= 0 ? path.substring(pathStart) : "/";
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        int slash = path.lastIndexOf('/');
        String segment = slash >= 0 ? path.substring(slash + 1) : path;
        if (segment.isBlank()) {
            return null;
        }
        if (segment.endsWith(".html")) {
            segment = segment.substring(0, segment.length() - 5);
        }
        return segment.toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().strip();
        return s.isEmpty() ? null : s;
    }

    private GitFrontMatterResolver() {}
}
