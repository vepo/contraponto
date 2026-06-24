package dev.vepo.contraponto.tag;

import dev.vepo.contraponto.rss.RssFeedPaths;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class TagTemplateExtensions {

    @TemplateExtension
    public static String rssFeedUrl(Tag tag) {
        return tag == null ? null : RssFeedPaths.tagFeed(tag);
    }

    @TemplateExtension
    public static String tagGridLoadMorePath(String tagSlug) {
        return "/tags/%s/components/grid".formatted(tagSlug);
    }

    @TemplateExtension
    public static String url(Tag tag) {
        return TagPaths.url(tag);
    }

    private TagTemplateExtensions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
