package dev.vepo.contraponto.git;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Resolved directory layout derived from documented defaults
 * ({@linkplain #defaults()}) optionally overridden via {@code _contraponto.yml}
 * in the repo root (see docs).
 */
public final class JekyllLayoutConvention {

    public static final String CONFIG_FILENAME = "_contraponto.yml";
    /**
     * Front matter key used to correlate Contraponto DB rows across renames/copies.
     */
    public static final String FM_POST_ID = "contraponto_post_id";

    public static JekyllLayoutConvention defaults() {
        return new JekyllLayoutConvention("_posts", "_drafts", "assets/images", "layout", "post");
    }

    /**
     * Merges non-blank YAML string fields into defaults. Unexpected keys are
     * ignored.
     *
     * <p>
     * Supported keys: {@code posts_directory}, {@code drafts_directory},
     * {@code assets_directory}, {@code layout_fm_key}, {@code default_layout}.
     */
    public static JekyllLayoutConvention fromYaml(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return defaults();
        }
        JekyllLayoutConvention d = defaults();
        return new JekyllLayoutConvention(txt(map.get("posts_directory"), d.postsDir),
                                          txt(map.get("drafts_directory"), d.draftsDir),
                                          txt(map.get("assets_directory"), d.assetsDir),
                                          txt(map.get("layout_fm_key"), d.layoutFmKey),
                                          txt(map.get("default_layout"), d.defaultLayoutValue));
    }

    public static Locale locale() {
        return Locale.ROOT;
    }

    private static String sanitizeDir(String s) {
        String t = s.replace('\\', '/').trim();
        while (t.startsWith("/")) {
            t = t.substring(1);
        }
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        if (t.contains("..")) {
            throw new IllegalArgumentException("Directory segments must not contain '..'.");
        }
        return t;
    }

    private static String txt(Object raw, String def) {
        if (raw == null) {
            return def;
        }
        String v = raw.toString().trim();
        return v.isEmpty() ? def : v;
    }

    private final String postsDir;

    private final String draftsDir;

    private final String assetsDir;

    private final String layoutFmKey;

    private final String defaultLayoutValue;

    private JekyllLayoutConvention(String postsDir,
                                   String draftsDir,
                                   String assetsDir,
                                   String layoutFmKey,
                                   String defaultLayoutValue) {
        this.postsDir = sanitizeDir(postsDir);
        this.draftsDir = sanitizeDir(draftsDir);
        this.assetsDir = sanitizeDir(assetsDir);
        this.layoutFmKey = layoutFmKey;
        this.defaultLayoutValue = defaultLayoutValue;
    }

    public String assetsRelative() {
        return assetsDir;
    }

    public String defaultLayoutValue() {
        return defaultLayoutValue;
    }

    public String draftsRelative() {
        return draftsDir;
    }

    public String layoutFrontMatterKey() {
        return layoutFmKey;
    }

    public String postsRelative() {
        return postsDir;
    }

    public Path resolveAssets(Path repoRoot) {
        return repoRoot.resolve(assetsDir).normalize();
    }

    public Path resolveDrafts(Path repoRoot) {
        return repoRoot.resolve(draftsDir).normalize();
    }

    public Path resolvePosts(Path repoRoot) {
        return repoRoot.resolve(postsDir).normalize();
    }
}
