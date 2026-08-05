package dev.vepo.contraponto.reddit.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import dev.vepo.contraponto.reddit.RedditSettings;

@ApplicationScoped
public class RedditPlatformManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(boolean redditConfigEnabled, boolean redditPlatformEnabled);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final RedditSettings settings;

    @Inject
    public RedditPlatformManageEndpoint(RedditSettings settings) {
        this.settings = settings;
    }

    public TemplateInstance renderHubPanel() {
        return Templates.panel(settings.configEnabled(), settings.platformSyndicationEnabled());
    }
}
