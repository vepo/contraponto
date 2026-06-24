package dev.vepo.contraponto.rss;

import io.quarkus.qute.TemplateGlobal;

@TemplateGlobal
public class RssTemplateGlobals {

    @TemplateGlobal(name = "siteRssFeedUrl")
    public static String siteRssFeedUrl() {
        return RssFeedPaths.siteFeed();
    }

    private RssTemplateGlobals() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
