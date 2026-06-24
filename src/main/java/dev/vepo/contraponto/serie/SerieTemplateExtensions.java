package dev.vepo.contraponto.serie;

import dev.vepo.contraponto.rss.RssFeedPaths;
import io.quarkus.qute.TemplateExtension;

@TemplateExtension
public class SerieTemplateExtensions {

    @TemplateExtension
    public static String rssFeedUrl(Serie serie) {
        return serie == null ? null : RssFeedPaths.serieFeed(serie);
    }

    @TemplateExtension
    public static String url(Serie serie) {
        return SeriePaths.extractUrl(serie);
    }

    private SerieTemplateExtensions() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
