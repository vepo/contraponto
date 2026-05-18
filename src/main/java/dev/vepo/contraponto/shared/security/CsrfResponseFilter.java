package dev.vepo.contraponto.shared.security;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CsrfResponseFilter implements ContainerResponseFilter {

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
        NewCookie cookie = new NewCookie.Builder(CsrfTokenService.COOKIE_NAME)
                                                                              .value(token.toString())
                                                                              .path("/")
                                                                              .httpOnly(false)
                                                                              .sameSite(NewCookie.SameSite.STRICT)
                                                                              .build();
        responseContext.getHeaders().add("Set-Cookie", cookie.toString());
    }
}
