package dev.vepo.contraponto.components;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.blog.BlogPublicUrlService;
import dev.vepo.contraponto.user.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("components/write-btn")
public class WriteButtonEndpoint {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance writeBtn(LoggedUser user, String writeUrl, boolean writeUsesHtmx);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final LoggedUser loggedUser;

    private final BlogPublicUrlService blogPublicUrlService;

    @Inject
    public WriteButtonEndpoint(LoggedUser loggedUser, BlogPublicUrlService blogPublicUrlService) {
        this.loggedUser = loggedUser;
        this.blogPublicUrlService = blogPublicUrlService;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance menu() {
        var writeUrl = blogPublicUrlService.workspaceMenuUrl("/write");
        var writeUsesHtmx = !blogPublicUrlService.usesPlatformForWorkspaceLinks();
        return Templates.writeBtn(loggedUser, writeUrl, writeUsesHtmx);
    }
}
