package dev.vepo.contraponto.shared.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;

@ApplicationScoped
public class LocaleCookieSupport {

    private static final int MAX_AGE_SECONDS = 60 * 60 * 24 * 365;

    public NewCookie buildLocaleCookie(String locale) {
        return new NewCookie.Builder(LocalePreference.COOKIE_NAME)
                                                                  .value(locale)
                                                                  .path("/")
                                                                  .maxAge(MAX_AGE_SECONDS)
                                                                  .build();
    }
}
