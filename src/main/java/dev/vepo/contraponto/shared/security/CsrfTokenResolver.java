package dev.vepo.contraponto.shared.security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;

@RequestScoped
public class CsrfTokenResolver {

    private final ContainerRequestContext requestContext;
    private final CsrfTokenService csrfTokenService;

    @Inject
    public CsrfTokenResolver(ContainerRequestContext requestContext, CsrfTokenService csrfTokenService) {
        this.requestContext = requestContext;
        this.csrfTokenService = csrfTokenService;
    }

    public String currentToken() {
        Object token = requestContext.getProperty(CsrfRequestSetupFilter.TOKEN_PROPERTY);
        if (token != null && !token.toString().isBlank()) {
            return token.toString();
        }
        return csrfTokenService.readToken(requestContext.getCookies());
    }
}
