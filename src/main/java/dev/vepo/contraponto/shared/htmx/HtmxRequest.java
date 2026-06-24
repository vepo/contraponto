package dev.vepo.contraponto.shared.htmx;

import jakarta.enterprise.context.RequestScoped;

/**
 * Whether the active request is an HTMX partial fetch
 * ({@code HX-Request: true}).
 */
@RequestScoped
public class HtmxRequest {

    public static final String REDIRECT_HEADER = "HX-Redirect";

    public static final String REQUEST_HEADER = "HX-Request";

    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
