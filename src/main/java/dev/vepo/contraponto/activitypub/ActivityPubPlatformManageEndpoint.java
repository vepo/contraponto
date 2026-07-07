package dev.vepo.contraponto.activitypub;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubPlatformManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance panel(boolean activityPubConfigEnabled, boolean activityPubPlatformEnabled);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final ActivityPubSettings settings;

    @Inject
    public ActivityPubPlatformManageEndpoint(ActivityPubSettings settings) {
        this.settings = settings;
    }

    public TemplateInstance renderHubPanel() {
        return Templates.panel(settings.configEnabled(), settings.enabled());
    }
}
