package dev.vepo.contraponto.blog;

import io.quarkus.qute.TemplateGlobal;
import jakarta.enterprise.inject.spi.CDI;

@TemplateGlobal
public class BlogTemplateGlobals {

    @TemplateGlobal(name = "applicationHomeUrl")
    public static String applicationHomeUrl() {
        return selectPublicUrlService().applicationHomeUrl();
    }

    @TemplateGlobal(name = "applicationHomeUsesHtmx")
    public static boolean applicationHomeUsesHtmx() {
        return selectPublicUrlService().applicationHomeUsesHtmx();
    }

    @TemplateGlobal(name = "platformOrigin")
    public static String platformOrigin() {
        var config = CDI.current().select(BlogSubdomainConfig.class);
        if (!config.isResolvable() || !config.get().enabled()) {
            return "";
        }
        var platformUrl = config.get().platformUrl("/");
        if (platformUrl.endsWith("/")) {
            return platformUrl.substring(0, platformUrl.length() - 1);
        }
        return platformUrl;
    }

    private static BlogPublicUrlService selectPublicUrlService() {
        return CDI.current().select(BlogPublicUrlService.class).get();
    }

    private BlogTemplateGlobals() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
