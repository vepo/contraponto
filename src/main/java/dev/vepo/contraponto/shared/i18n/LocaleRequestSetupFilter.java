package dev.vepo.contraponto.shared.i18n;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LocaleRequestSetupFilter implements ContainerRequestFilter {

    private final LocalePreference localePreference;
    private final CurrentLocale currentLocale;

    @Inject
    public LocaleRequestSetupFilter(LocalePreference localePreference, CurrentLocale currentLocale) {
        this.localePreference = localePreference;
        this.currentLocale = currentLocale;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var cookies = requestContext.getCookies();
        String raw = null;
        if (cookies != null && cookies.containsKey(LocalePreference.COOKIE_NAME)) {
            var cookie = cookies.get(LocalePreference.COOKIE_NAME);
            if (cookie != null) {
                raw = cookie.getValue();
            }
        }
        currentLocale.set(localePreference.normalize(raw));
    }
}
