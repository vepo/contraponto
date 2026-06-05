package dev.vepo.contraponto.shared.security;

import java.io.IOException;
import java.util.Optional;

import dev.vepo.contraponto.shared.infra.SiteIntegration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class SecurityHeadersFilter implements ContainerResponseFilter {

    static String contentSecurityPolicy(Optional<String> siteIntegrationOrigin) {
        String integrationOrigin = siteIntegrationOrigin.map(origin -> " " + origin).orElse("");
        return """
               default-src 'self'; \
               script-src 'self' 'unsafe-inline' https://platform.twitter.com%s; \
               style-src 'self' 'unsafe-inline' https://platform.twitter.com; \
               img-src 'self' data: https://pbs.twimg.com https://abs.twimg.com https://syndication.twitter.com https://github.com https://avatars.githubusercontent.com; \
               font-src 'self' https://platform.twitter.com; \
               frame-src https://platform.twitter.com https://www.youtube.com https://www.youtube-nocookie.com; \
               connect-src 'self'%s; \
               form-action 'self'; \
               frame-ancestors 'none'""".formatted(integrationOrigin,
                                                   integrationOrigin);
    }

    private final String contentSecurityPolicy;

    @Inject
    public SecurityHeadersFilter(SiteIntegration siteIntegration) {
        this.contentSecurityPolicy = contentSecurityPolicy(siteIntegration.scriptOrigin());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        var headers = responseContext.getHeaders();
        headers.putSingle("Content-Security-Policy", contentSecurityPolicy.replace('\n', ' ').trim());
        headers.putSingle("X-Content-Type-Options", "nosniff");
        headers.putSingle("X-Frame-Options", "DENY");
        headers.putSingle("Referrer-Policy", "strict-origin-when-cross-origin");
    }
}
