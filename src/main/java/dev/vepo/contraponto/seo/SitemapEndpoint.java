package dev.vepo.contraponto.seo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/sitemap.xml")
@ApplicationScoped
public class SitemapEndpoint {

    private final SitemapService sitemapService;

    @Inject
    public SitemapEndpoint(SitemapService sitemapService) {
        this.sitemapService = sitemapService;
    }

    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response sitemap() {
        return Response.ok(sitemapService.renderXml())
                       .header("Cache-Control", "public, max-age=3600")
                       .build();
    }
}
