package dev.vepo.contraponto.seo;

import org.eclipse.microprofile.openapi.annotations.Operation;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/components/seo")
@ApplicationScoped
public class SeoComponentEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance headFragment(SeoMetadata seo);

        public static native TemplateInstance oob(SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final SeoService seoService;

    @Inject
    public SeoComponentEndpoint(SeoService seoService) {
        this.seoService = seoService;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance seo(@QueryParam("path") @DefaultValue("/") String path) {
        return Templates.oob(seoService.resolveFromPath(path));
    }
}
