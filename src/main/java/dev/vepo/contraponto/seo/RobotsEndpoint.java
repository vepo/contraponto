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
        var lines = new StringBuilder();
        lines.append("User-agent: *\n");
        lines.append("Allow: /\n");
        for (String rule : CrawlerPrivatePaths.disallowRules()) {
            lines.append("Disallow: ").append(rule).append('\n');
        }
        lines.append("Disallow: /feed\n");
        lines.append("Sitemap: ").append(publicSiteUrl.baseUrl()).append("/sitemap.xml");
        return Response.ok(lines.toString()).build();
    }
}
