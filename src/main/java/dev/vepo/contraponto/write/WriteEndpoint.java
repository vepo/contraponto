package dev.vepo.contraponto.write;

import java.time.LocalDateTime;
import java.util.Objects;

import dev.vepo.contraponto.post.PostRepository;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/write")
@ApplicationScoped
public class WriteEndpoint {

    private final PostRepository postRepository;
    private final Template write;

    @Inject
    public WriteEndpoint(PostRepository postRepository, Template write) {
        this.postRepository = postRepository;
        this.write = write;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance write(@QueryParam("edit") Long editId) {
        var instance = write.data("currentYear", LocalDateTime.now().getYear());

        if (Objects.nonNull(editId)) {
            postRepository.findById(editId).ifPresentOrElse(post -> instance.data("post", post),
                                                            () -> instance.data("post", null));
        } else {
            instance.data("post", null);
        }

        return instance;
    }
}