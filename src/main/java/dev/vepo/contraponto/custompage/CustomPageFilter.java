package dev.vepo.contraponto.custompage;

import java.io.IOException;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

@PreMatching
@Provider
@Priority(Priorities.ENTITY_CODER)
public class CustomPageFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var method = requestContext.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return;
        }

        var uriInfo = requestContext.getUriInfo();
        var segments = uriInfo.getPathSegments();
        var pageType = CustomPagePaths.matchPageType(segments);
        if (pageType == PageType.NONE) {
            return;
        }

        var internalPath = switch (pageType) {
            case GLOBAL -> CustomPagePaths.internalUrl(pageType, CustomPagePaths.slug(segments, pageType));
            case USER -> CustomPagePaths.internalUrl(pageType,
                                                     CustomPagePaths.username(segments),
                                                     CustomPagePaths.slug(segments, pageType));
            case BLOG -> CustomPagePaths.internalUrl(pageType,
                                                     CustomPagePaths.username(segments),
                                                     CustomPagePaths.blogSlug(segments),
                                                     CustomPagePaths.slug(segments, pageType));
            default -> throw new IllegalStateException("Unexpected page type: %s".formatted(pageType));
        };

        var targetUri = UriBuilder.fromUri(uriInfo.getBaseUri())
                                  .path(internalPath.substring(1))
                                  .replaceQuery(uriInfo.getRequestUri().getQuery())
                                  .build();

        requestContext.setRequestUri(uriInfo.getBaseUri(), targetUri);
    }
}
