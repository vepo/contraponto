package dev.vepo.contraponto.shared.security;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CsrfRequestSetupFilter implements ContainerRequestFilter {

    public static final String TOKEN_PROPERTY = "contraponto.csrf.token";
    public static final String NEW_TOKEN_PROPERTY = "contraponto.csrf.new";

    private final CsrfTokenService csrfTokenService;
    private final CurrentCsrfToken currentCsrfToken;

    @Inject
    public CsrfRequestSetupFilter(CsrfTokenService csrfTokenService, CurrentCsrfToken currentCsrfToken) {
        this.csrfTokenService = csrfTokenService;
        this.currentCsrfToken = currentCsrfToken;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String existing = csrfTokenService.readToken(requestContext.getCookies());
        if (!existing.isBlank()) {
            requestContext.setProperty(TOKEN_PROPERTY, existing);
            requestContext.setProperty(NEW_TOKEN_PROPERTY, false);
            currentCsrfToken.set(existing);
            return;
        }
        String token = csrfTokenService.newToken();
        requestContext.setProperty(TOKEN_PROPERTY, token);
        requestContext.setProperty(NEW_TOKEN_PROPERTY, true);
        currentCsrfToken.set(token);
    }
}
