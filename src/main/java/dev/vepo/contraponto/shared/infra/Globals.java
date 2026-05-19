package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;

import dev.vepo.contraponto.notification.NotificationHtmxConfig;
import dev.vepo.contraponto.rss.RssFeedPaths;
import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.shared.i18n.CurrentLocale;
import dev.vepo.contraponto.shared.i18n.LocalePreference;
import dev.vepo.contraponto.shared.security.CurrentCsrfToken;
import io.quarkus.qute.TemplateGlobal;
import jakarta.enterprise.inject.spi.CDI;

@TemplateGlobal
public class Globals {

    @TemplateGlobal(name = "authRefreshTrigger")
    public static String authRefreshTrigger() {
        return HtmxTriggers.AUTH_REFRESH_TRIGGER;
    }

    @TemplateGlobal(name = "csrfToken")
    public static String csrfToken() {
        var current = CDI.current().select(CurrentCsrfToken.class);
        if (!current.isResolvable()) {
            return "";
        }
        return current.get().get();
    }

    @TemplateGlobal(name = "currentYear")
    public static int currentYear() {
        return LocalDateTime.now().getYear();
    }

    @TemplateGlobal(name = "locale")
    public static String locale() {
        var current = CDI.current().select(CurrentLocale.class);
        if (!current.isResolvable()) {
            return LocalePreference.DEFAULT_LOCALE;
        }
        return current.get().get();
    }

    @TemplateGlobal(name = "localeLang")
    public static String localeLang() {
        var current = CDI.current().select(CurrentLocale.class);
        if (!current.isResolvable()) {
            return "pt-BR";
        }
        var locale = current.get().get();
        return CDI.current().select(LocalePreference.class).get().toBcp47(locale);
    }

    @TemplateGlobal(name = "notificationBadgeTrigger")
    public static String notificationBadgeTrigger() {
        return CDI.current().select(NotificationHtmxConfig.class).get().badgeTrigger();
    }

    @TemplateGlobal(name = "session")
    public static LoggedUser session() {
        var loggedUser = CDI.current().select(LoggedUser.class);
        if (!loggedUser.isResolvable()) {
            return new LoggedUser();
        }
        return loggedUser.get();
    }

    @TemplateGlobal(name = "siteRssFeedUrl")
    public static String siteRssFeedUrl() {
        return RssFeedPaths.siteFeed();
    }

    private Globals() {
        /* This utility class should not be instantiated */
    }
}
