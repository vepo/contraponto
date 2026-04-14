package dev.vepo.contraponto.write;

import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.UserContext.UserInfo;
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

    private static final Logger logger = LoggerFactory.getLogger(WriteEndpoint.class);
    private final PostRepository postRepository;
    private final Template write;
    private final UserInfo userInfo;

    @Inject
    public WriteEndpoint(PostRepository postRepository, Template write, UserInfo userInfo) {
        this.postRepository = postRepository;
        this.write = write;
        this.userInfo = userInfo;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance write(@QueryParam("edit") Long editId) {
        logger.info("Autenticated user={}", userInfo);
        var instance = write.data("currentYear", LocalDateTime.now().getYear())
                            .data("user", userInfo);

        if (Objects.nonNull(editId)) {
            postRepository.findById(editId).ifPresentOrElse(post -> instance.data("post", post),
                                                            () -> instance.data("post", null));
        } else {
            instance.data("post", null);
        }

        return instance;
    }
}