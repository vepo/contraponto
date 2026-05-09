package dev.vepo.contraponto.custompage;

import java.io.IOException;
import java.util.stream.Collectors;

import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CustomPageFilter implements ContainerRequestFilter {
    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance page(CustomPage page, Links links, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

    }

    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public CustomPageFilter(CustomPageRepository customPageRepository, LoggedUser loggedUser) {
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        var maybePage = this.customPageRepository.findBySlug(requestContext.getUriInfo().getPath());
        if (maybePage.isPresent()) {
            requestContext.abortWith(Response.ok().entity(Templates.page(maybePage.get(), customPageRepository.loadLinks(), loggedUser)).build());
            return;
        }

        if (requestContext.getUriInfo().getPathSegments().size() > 1) {
            maybePage = this.customPageRepository.findByUsernameAndSlug(requestContext.getUriInfo()
                                                                                      .getPathSegments()
                                                                                      .get(0)
                                                                                      .getPath(),
                                                                        requestContext.getUriInfo()
                                                                                      .getPathSegments()
                                                                                      .stream()
                                                                                      .skip(1)
                                                                                      .map(PathSegment::getPath)
                                                                                      .collect(Collectors.joining("/")));
            if (maybePage.isPresent()) {
                requestContext.abortWith(Response.ok().entity(Templates.page(maybePage.get(), customPageRepository.loadLinks(), loggedUser)).build());
            }
        }
    }
}
