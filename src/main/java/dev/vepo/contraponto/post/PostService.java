package dev.vepo.contraponto.post;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("post/{id}")
public class PostService {
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String post(@PathParam("id") String slug) {
        return slug;
    }
}
