package dev.vepo.contraponto.git;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.vepo.contraponto.renderer.Format;

/**
 * Discovers image asset paths referenced by a post (front matter + body) for
 * post-guided Git import.
 */
final class GitPostAssetReferences {

    /**
     * AsciiDoc block/inline image macros: {@code image::path[attrs]} or
     * {@code image:path[attrs]}.
     */
    private static final Pattern ASCIIDOC_IMAGE =
            Pattern.compile("image::?([^\\s\\[]+)\\[", Pattern.CASE_INSENSITIVE);

    private static void addNormalizedPath(String rawPath, JekyllLayoutConvention convention, Set<String> paths) {
        if (rawPath == null || rawPath.isBlank()) {
            return;
        }
        String path = rawPath.strip().replace('\\', '/');
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String assetsPrefix = convention.assetsRelative();
        if (path.startsWith(assetsPrefix + "/")) {
            path = path.substring(assetsPrefix.length() + 1);
        }
        if (path.isBlank() || path.contains("://")) {
            return;
        }
        int dot = path.lastIndexOf('.');
        if (dot <= 0 || dot == path.length() - 1) {
            return;
        }
        paths.add(path);
    }

    static String basenameWithoutExtension(String assetRelativeWithExt) {
        int dot = assetRelativeWithExt.lastIndexOf('.');
        return dot > 0 ? assetRelativeWithExt.substring(0, dot) : assetRelativeWithExt;
    }

    /**
     * Paths relative to {@link JekyllLayoutConvention#assetsRelative()} (may
     * include subdirectories), each including its file extension e.g.
     * {@code capas/foo.webp}.
     */
    static Set<String> collectAssetRelativePaths(String coverPath,
                                                 String body,
                                                 Format format,
                                                 JekyllLayoutConvention convention) {
        Set<String> paths = new LinkedHashSet<>();
        addNormalizedPath(coverPath, convention, paths);
        if (body != null && !body.isBlank()) {
            Matcher markdown = GitImageSyncService.relativeAssetPattern(convention).matcher(body);
            while (markdown.find()) {
                paths.add(markdown.group(1) + markdown.group(2));
            }
            if (format == Format.ASCIIDOC) {
                Matcher adoc = ASCIIDOC_IMAGE.matcher(body);
                while (adoc.find()) {
                    addNormalizedPath(adoc.group(1).strip(), convention, paths);
                }
            }
        }
        return paths;
    }

    static String extensionWithDot(String assetRelativeWithExt) {
        int dot = assetRelativeWithExt.lastIndexOf('.');
        return dot >= 0 ? assetRelativeWithExt.substring(dot).toLowerCase(Locale.ROOT) : ".png";
    }

    private GitPostAssetReferences() {}
}
