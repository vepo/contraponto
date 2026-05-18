package dev.vepo.contraponto.shared.security;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    private static final String CSP = """
                                      default-src 'self'; \
                                      script-src 'self' 'unsafe-inline'; \
                                      style-src 'self' 'unsafe-inline'; \
                                      img-src 'self' data:; \
                                      font-src 'self'; \
                                      connect-src 'self'; \
                                      form-action 'self'; \
                                      frame-ancestors 'none'""";

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        var headers = responseContext.getHeaders();
        headers.putSingle("Content-Security-Policy", CSP.replace('\n', ' ').trim());
        headers.putSingle("X-Content-Type-Options", "nosniff");
        headers.putSingle("X-Frame-Options", "DENY");
        headers.putSingle("Referrer-Policy", "strict-origin-when-cross-origin");
    }
}
