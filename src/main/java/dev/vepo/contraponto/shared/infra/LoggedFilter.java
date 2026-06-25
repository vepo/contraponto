package dev.vepo.contraponto.shared.infra;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.htmx.HtmxRequest;
import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.shared.security.SessionConstants;
import dev.vepo.contraponto.user.LoggedUserProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

@Logged
@Provider
public class LoggedFilter implements ContainerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggedFilter.class);

    private static boolean isHtmxRequest(ContainerRequestContext requestContext) {
        return "true".equalsIgnoreCase(requestContext.getHeaderString(HtmxRequest.REQUEST_HEADER));
    }

    private static String requestReturnPath(ContainerRequestContext requestContext) {
        var path = requestContext.getUriInfo().getPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        if (!path.startsWith("/")) {
            path = "/%s".formatted(path);
        }
        var query = requestContext.getUriInfo().getRequestUri().getQuery();
        if (query == null || query.isBlank()) {
            return path;
        }
        return "%s?%s".formatted(path, query);
    }

    static String safeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank() || !returnTo.startsWith("/") || returnTo.startsWith("//")) {
            return "/";
        }
        return returnTo;
    }

    private final LoggedUserProvider loggedUserProvider;

    @Inject
    public LoggedFilter(LoggedUserProvider loggedUserProvider) {
        this.loggedUserProvider = loggedUserProvider;
    }

    private void abortUnauthenticated(ContainerRequestContext requestContext) {
        var returnTo = safeReturnTo(requestReturnPath(requestContext));
        if (isHtmxRequest(requestContext)) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                                             .header(HtmxTriggers.HEADER_AFTER_SETTLE, HtmxTriggers.loginRequired(returnTo))
                                             .build());
            return;
        }
        requestContext.abortWith(Response.seeOther(UriBuilder.fromPath("/")
                                                             .queryParam("signIn", "1")
                                                             .queryParam("returnTo", returnTo)
                                                             .build())
                                         .build());
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Optional.ofNullable(requestContext.getCookies().get(SessionConstants.SESSION_COOKIE_NAME))
                .flatMap(sessionId -> loggedUserProvider.find(sessionId.getValue()))
                .ifPresentOrElse(user -> logger.info("User found! user={}", user),
                                 () -> abortUnauthenticated(requestContext));
    }
}
