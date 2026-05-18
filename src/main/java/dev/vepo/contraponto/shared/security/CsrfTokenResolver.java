package dev.vepo.contraponto.shared.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

@RequestScoped
public class CsrfTokenResolver {

    private final CurrentCsrfToken currentCsrfToken;

    @Inject
    public CsrfTokenResolver(CurrentCsrfToken currentCsrfToken) {
        this.currentCsrfToken = currentCsrfToken;
    }

    public String currentToken() {
        return currentCsrfToken.get();
    }
}
