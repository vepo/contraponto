package dev.vepo.contraponto.seo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/robots.txt")
@ApplicationScoped
public class RobotsEndpoint {

    private final PublicSiteUrl publicSiteUrl;

    @Inject
    public RobotsEndpoint(PublicSiteUrl publicSiteUrl) {
        this.publicSiteUrl = publicSiteUrl;
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response robots() {
        String body = """
                      User-agent: *
                      Allow: /
                      Disallow: /write
                      Disallow: /writing
                      Disallow: /manage
                      Disallow: /account
                      Disallow: /administration
                      Disallow: /editor
                      Disallow: /forms/
                      Disallow: /api/
                      Disallow: /auth/
                      Disallow: /search
                      Sitemap: %s/sitemap.xml
                      """.formatted(publicSiteUrl.baseUrl()).stripTrailing();
        return Response.ok(body).build();
    }
}
