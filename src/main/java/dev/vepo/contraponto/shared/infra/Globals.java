package dev.vepo.contraponto.shared.infra;

import java.time.LocalDateTime;
import java.time.ZoneId;

import dev.vepo.contraponto.shared.htmx.HtmxRequest;
import dev.vepo.contraponto.shared.i18n.CurrentLocale;
import dev.vepo.contraponto.shared.i18n.LocalePreference;
import dev.vepo.contraponto.shared.security.CurrentCsrfToken;
import io.quarkus.qute.TemplateGlobal;
import jakarta.enterprise.inject.spi.CDI;

@TemplateGlobal
public class Globals {

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
        return LocalDateTime.now(ZoneId.systemDefault()).getYear();
    }

    @TemplateGlobal(name = "htmxRequest")
    public static boolean htmxRequest() {
        var current = CDI.current().select(HtmxRequest.class);
        if (!current.isResolvable()) {
            return false;
        }
        return current.get().isActive();
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

    @TemplateGlobal(name = "pageAssets")
    public static PageAssets pageAssets() {
        var current = CDI.current().select(CurrentPageAssets.class);
        if (!current.isResolvable()) {
            return PageAssets.PUBLIC_READ;
        }
        return current.get().get();
    }

    @TemplateGlobal(name = "siteIntegrationEnabled")
    public static boolean siteIntegrationEnabled() {
        return CDI.current().select(SiteIntegration.class).get().enabled();
    }

    @TemplateGlobal(name = "siteIntegrationScriptDataToken")
    public static String siteIntegrationScriptDataToken() {
        return CDI.current().select(SiteIntegration.class).get().scriptDataToken();
    }

    @TemplateGlobal(name = "siteIntegrationScriptUrl")
    public static String siteIntegrationScriptUrl() {
        return CDI.current().select(SiteIntegration.class).get().scriptUrl();
    }

    @TemplateGlobal(name = "siteName")
    public static String siteName() {
        return CDI.current().select(SiteBranding.class).get().displayName();
    }

    @TemplateGlobal(name = "siteSeoName")
    public static String siteSeoName() {
        return CDI.current().select(SiteBranding.class).get().seoName();
    }

    private Globals() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
