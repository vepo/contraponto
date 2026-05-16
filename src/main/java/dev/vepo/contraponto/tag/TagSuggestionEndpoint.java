package dev.vepo.contraponto.tag;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.shared.infra.Logged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/forms/write/tag-suggestions")
@ApplicationScoped
public class TagSuggestionEndpoint {

    private final TagRepository tagRepository;

    @Inject
    public TagSuggestionEndpoint(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> suggest(@QueryParam("q") String q) {
        return tagRepository.suggestNames(q == null ? "" : q, 25);
    }
}
