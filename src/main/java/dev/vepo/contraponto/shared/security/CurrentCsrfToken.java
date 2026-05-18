package dev.vepo.contraponto.shared.security;

import jakarta.enterprise.context.RequestScoped;

/**
 * CSRF token for the active HTTP request, set by {@link CsrfRequestSetupFilter}
 * and read from Qute templates (including {@code @TemplateGlobal}).
 */
@RequestScoped
public class CurrentCsrfToken {

    private String value = "";

    public String get() {
        return value;
    }

    public void set(String value) {
        this.value = value != null ? value : "";
    }
}
