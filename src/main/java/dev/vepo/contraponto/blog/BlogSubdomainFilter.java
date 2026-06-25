package dev.vepo.contraponto.blog;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import java.net.URI;

import dev.vepo.contraponto.shared.htmx.HtmxRequest;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

@PreMatching
@Provider
@Priority(900)
public class BlogSubdomainFilter implements ContainerRequestFilter {

    private static Optional<String> firstPathSegment(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return Optional.empty();
        }
        List<String> segments = Arrays.stream(path.split("/"))
                                      .filter(segment -> !segment.isBlank())
                                      .toList();
        if (segments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(segments.get(0));
    }

    private static boolean isHtmxRequest(ContainerRequestContext requestContext) {
        return "true".equalsIgnoreCase(requestContext.getHeaderString(HtmxRequest.REQUEST_HEADER));
    }

    private static String prependUsername(String username, String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "/%s".formatted(username);
        }
        if (path.startsWith("/")) {
            return "/%s%s".formatted(username, path);
        }
        return "/%s/%s".formatted(username, path);
    }

    private final BlogSubdomainConfig config;

    private final BlogSubdomainContext context;

    @Inject
    public BlogSubdomainFilter(BlogSubdomainConfig config, BlogSubdomainContext context) {
        this.config = config;
        this.context = context;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!config.enabled()) {
            return;
        }
        var method = requestContext.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return;
        }

        var host = requestContext.getHeaderString("Host");
        var username = config.parseUserSubdomain(host);
        if (username.isEmpty()) {
            return;
        }

        var authorUsername = username.get();
        context.activate(authorUsername);

        var uriInfo = requestContext.getUriInfo();
        var path = uriInfo.getPath();
        if (path == null) {
            path = "/";
        }

        var subdomainPath = config.normalizeAuthorSubdomainRequestPath(authorUsername, path);

        if (config.shouldSkipSubdomainRewrite(subdomainPath)) {
            return;
        }

        var firstSegment = firstPathSegment(subdomainPath);
        if (firstSegment.isPresent() && config.isPlatformOnlyRootSegment(firstSegment.get())) {
            var platformUrl = config.platformUrl(path);
            if (isHtmxRequest(requestContext)) {
                requestContext.abortWith(Response.ok()
                                                 .header(HtmxRequest.REDIRECT_HEADER, platformUrl)
                                                 .build());
            } else {
                requestContext.abortWith(Response.status(Response.Status.FOUND)
                                                 .location(URI.create(platformUrl))
                                                 .build());
            }
            return;
        }

        var internalPath = prependUsername(authorUsername, subdomainPath);
        var targetUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                                  .path(internalPath.startsWith("/") ? internalPath.substring(1) : internalPath)
                                  .replaceQuery(uriInfo.getRequestUri().getQuery())
                                  .build();
        requestContext.setRequestUri(uriInfo.getBaseUri(), targetUri);
    }
}
