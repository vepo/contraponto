package dev.vepo.contraponto.shared.htmx;

import jakarta.enterprise.context.RequestScoped;

/**
 * Whether the active request is an HTMX partial fetch
 * ({@code HX-Request: true}).
 */
@RequestScoped
public class HtmxRequest {

    private boolean active;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
