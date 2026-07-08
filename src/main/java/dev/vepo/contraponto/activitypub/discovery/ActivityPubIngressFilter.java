package dev.vepo.contraponto.activitypub.discovery;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;
import dev.vepo.contraponto.blog.BlogSubdomainContext;

/**
 * Pre-matching filter that rewrites public federation URLs to
 * {@link ActivityPubIngressPaths#INTERNAL_PREFIX} before JAX-RS routing.
 * Preserves the public path in {@link BlogSubdomainContext} for HTTP signature
 * verification.
 */
@PreMatching
@Provider
@Priority(950)
public class ActivityPubIngressFilter implements ContainerRequestFilter {

    private final BlogSubdomainContext subdomainContext;

    /**
     * @param subdomainContext request-scoped holder for the external signature path
     *                         on inbox POST
     */
    @Inject
    public ActivityPubIngressFilter(BlogSubdomainContext subdomainContext) {
        this.subdomainContext = subdomainContext;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var uriInfo = requestContext.getUriInfo();
        var publicPath = uriInfo.getPath();
        var internalPath = ActivityPubIngressPaths.resolveInternalPath(publicPath);
        if (internalPath.isEmpty()) {
            return;
        }
        var path = internalPath.get();
        var query = uriInfo.getRequestUri().getQuery();
        // Preserve signature path set by BlogSubdomainFilter (e.g. POST /inbox on
        // author
        // subdomain). Remotes sign the external path they POST to — never the platform
        // rewrite (/vepo/inbox) or this internal prefix.
        if (subdomainContext.signatureRequestPath().isEmpty()) {
            var externalPath = publicPath.startsWith("/") ? publicPath : "/%s".formatted(publicPath);
            if (query != null && !query.isBlank()) {
                externalPath = "%s?%s".formatted(externalPath, query);
            }
            subdomainContext.setSignatureRequestPath(externalPath);
        }

        var targetUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                                  .path(path.substring(1))
                                  .replaceQuery(query)
                                  .build();
        requestContext.setRequestUri(uriInfo.getBaseUri(), targetUri);
    }
}
