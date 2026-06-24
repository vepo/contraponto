package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import io.quarkus.qute.TemplateGlobal;
import jakarta.enterprise.inject.spi.CDI;

@TemplateGlobal
public class NotificationTemplateGlobals {

    @TemplateGlobal(name = "authRefreshTrigger")
    public static String authRefreshTrigger() {
        return HtmxTriggers.AUTH_REFRESH_TRIGGER;
    }

    @TemplateGlobal(name = "notificationBadgeTrigger")
    public static String notificationBadgeTrigger() {
        return CDI.current().select(NotificationHtmxConfig.class).get().badgeTrigger();
    }

    private NotificationTemplateGlobals() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
