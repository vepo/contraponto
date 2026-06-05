package dev.vepo.contraponto.rss;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class RssNoIndexFilter implements ContainerResponseFilter {

    static boolean isFeedPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        return "/feed".equals(path) || path.endsWith("/feed");
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        if (isFeedPath(path)) {
            responseContext.getHeaders().putSingle("X-Robots-Tag", "noindex");
        }
    }
}
