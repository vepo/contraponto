package dev.vepo.contraponto.shared.infra;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PageAssetsFilter implements ContainerRequestFilter {

    private static boolean isManagePath(String path) {
        return path.startsWith("/manage")
                || path.startsWith("/writing")
                || path.startsWith("/library")
                || path.startsWith("/administration")
                || path.startsWith("/review")
                || path.startsWith("/editor")
                || path.startsWith("/blogs/")
                || path.startsWith("/users");
    }

    static PageAssets resolveProfile(String path) {
        if (path == null || path.isBlank()) {
            return PageAssets.PUBLIC_READ;
        }
        var normalized = path.startsWith("/") ? path : "/%s".formatted(path);
        if (normalized.equals("/write") || normalized.startsWith("/write/")) {
            return PageAssets.WRITE;
        }
        if (normalized.contains("/post/")) {
            return PageAssets.POST_READ;
        }
        if (isManagePath(normalized)) {
            return PageAssets.MANAGE;
        }
        return PageAssets.PUBLIC_READ;
    }

    private final CurrentPageAssets currentPageAssets;

    @Inject
    public PageAssetsFilter(CurrentPageAssets currentPageAssets) {
        this.currentPageAssets = currentPageAssets;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var path = requestContext.getUriInfo().getPath();
        currentPageAssets.set(resolveProfile(path));
    }
}
