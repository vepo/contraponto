package dev.vepo.contraponto.shared.i18n;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CurrentLocale {

    private String locale = LocalePreference.DEFAULT_LOCALE;

    public String get() {
        return locale;
    }

    public void set(String locale) {
        this.locale = locale;
    }
}
