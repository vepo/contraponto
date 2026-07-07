package dev.vepo.contraponto.blog;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import java.net.URI;

import dev.vepo.contraponto.shared.htmx.HtmxRequest;
import dev.vepo.contraponto.shared.infra.LoggedFilter;
import dev.vepo.contraponto.shared.security.SessionConstants;
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

    private static boolean hasSessionCookie(ContainerRequestContext requestContext) {
        var sessionCookie = requestContext.getCookies().get(SessionConstants.SESSION_COOKIE_NAME);
        return sessionCookie != null && sessionCookie.getValue() != null && !sessionCookie.getValue().isBlank();
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
        var uriInfo = requestContext.getUriInfo();
        var path = uriInfo.getPath();
        if (path == null) {
            path = "/";
        }

        if ("POST".equals(method) && config.isActivityPubInboxPost(method, path)) {
            rewriteAuthorSubdomainRequest(requestContext, path, uriInfo);
            return;
        }

        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return;
        }

        rewriteAuthorSubdomainRequest(requestContext, path, uriInfo);
    }

    private void redirectToPlatform(ContainerRequestContext requestContext, String path, jakarta.ws.rs.core.UriInfo uriInfo) {
        var query = uriInfo.getRequestUri().getQuery();
        var platformPath = path;
        if (query != null && !query.isBlank()) {
            platformPath = "%s?%s".formatted(path, query);
        }
        var platformUrl = config.platformUrl(platformPath);
        if (isHtmxRequest(requestContext)) {
            requestContext.abortWith(Response.ok()
                                             .header(HtmxRequest.REDIRECT_HEADER, platformUrl)
                                             .build());
        } else {
            requestContext.abortWith(Response.status(Response.Status.FOUND)
                                             .location(URI.create(platformUrl))
                                             .build());
        }
    }

    private void redirectToPlatformSignIn(ContainerRequestContext requestContext,
                                          String path,
                                          jakarta.ws.rs.core.UriInfo uriInfo) {
        var query = uriInfo.getRequestUri().getQuery();
        var returnPath = path;
        if (query != null && !query.isBlank()) {
            returnPath = "%s?%s".formatted(path, query);
        }
        var returnTo = LoggedFilter.safeReturnTo(returnPath);
        var signInUri = UriBuilder.fromUri(URI.create(config.platformUrl("/")))
                                  .queryParam("signIn", "1")
                                  .queryParam("returnTo", returnTo)
                                  .build();
        var signInUrl = signInUri.toString();
        if (isHtmxRequest(requestContext)) {
            requestContext.abortWith(Response.ok()
                                             .header(HtmxRequest.REDIRECT_HEADER, signInUrl)
                                             .build());
        } else {
            requestContext.abortWith(Response.status(Response.Status.SEE_OTHER)
                                             .location(signInUri)
                                             .build());
        }
    }

    private void rewriteAuthorSubdomainRequest(ContainerRequestContext requestContext,
                                               String path,
                                               jakarta.ws.rs.core.UriInfo uriInfo)
            throws IOException {
        var host = requestContext.getHeaderString("Host");
        var username = config.parseUserSubdomain(host);
        if (username.isEmpty()) {
            return;
        }

        var authorUsername = username.get();
        context.activate(authorUsername);

        var subdomainPath = config.normalizeAuthorSubdomainRequestPath(authorUsername, path);

        if (config.isActivityPubInboxPost(requestContext.getMethod(), subdomainPath)) {
            var query = uriInfo.getRequestUri().getQuery();
            var externalPath = subdomainPath.startsWith("/") ? subdomainPath : "/%s".formatted(subdomainPath);
            if (query != null && !query.isBlank()) {
                externalPath = "%s?%s".formatted(externalPath, query);
            }
            context.setSignatureRequestPath(externalPath);
        }

        if ("GET".equals(requestContext.getMethod()) || "HEAD".equals(requestContext.getMethod())) {
            if (config.isWorkspaceRootPath(subdomainPath)) {
                if (hasSessionCookie(requestContext)) {
                    redirectToPlatform(requestContext, subdomainPath, uriInfo);
                } else {
                    redirectToPlatformSignIn(requestContext, subdomainPath, uriInfo);
                }
                return;
            }

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
        }

        var internalPath = prependUsername(authorUsername, subdomainPath);
        var targetUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                                  .path(internalPath.startsWith("/") ? internalPath.substring(1) : internalPath)
                                  .replaceQuery(uriInfo.getRequestUri().getQuery())
                                  .build();
        requestContext.setRequestUri(uriInfo.getBaseUri(), targetUri);
    }
}
