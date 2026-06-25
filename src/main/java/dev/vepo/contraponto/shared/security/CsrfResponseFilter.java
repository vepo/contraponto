package dev.vepo.contraponto.shared.security;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CsrfResponseFilter implements ContainerResponseFilter {

    private final SessionCookieSupport sessionCookieSupport;

    @Inject
    public CsrfResponseFilter(SessionCookieSupport sessionCookieSupport) {
        this.sessionCookieSupport = sessionCookieSupport;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (responseContext.getStatus() >= 400) {
            return;
        }
        Object isNew = requestContext.getProperty(CsrfRequestSetupFilter.NEW_TOKEN_PROPERTY);
        if (!Boolean.TRUE.equals(isNew)) {
            return;
        }
        Object token = requestContext.getProperty(CsrfRequestSetupFilter.TOKEN_PROPERTY);
        if (token == null || token.toString().isBlank()) {
            return;
        }
        NewCookie cookie = sessionCookieSupport.buildCsrfNewCookie(token.toString());
        responseContext.getHeaders().add("Set-Cookie", cookie.toString());
    }
}
