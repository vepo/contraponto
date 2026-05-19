package dev.vepo.contraponto.notification;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationHtmxConfig {

    private final String badgePollInterval;
    private final int overlayLimit;

    public NotificationHtmxConfig(@ConfigProperty(name = "app.notifications.badge-poll-interval", defaultValue = "60s") String badgePollInterval,
                                  @ConfigProperty(name = "app.notifications.overlay-limit", defaultValue = "10") int overlayLimit) {
        this.badgePollInterval = badgePollInterval;
        this.overlayLimit = overlayLimit;
    }

    public String badgeTrigger() {
        return HtmxTriggers.notificationBadgeTrigger(badgePollInterval);
    }

    public int overlayLimit() {
        return overlayLimit;
    }
}
