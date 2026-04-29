package dev.vepo.contraponto.post;

import java.time.LocalDateTime;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.view.SessionIdProvider;
import dev.vepo.contraponto.view.ViewRepository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

@Path("{username}/post/{slug}")
@ApplicationScoped
public class PostEndpoint {
    @CheckedTemplate
    @SuppressWarnings("java:S1118")
    public static class Templates {
        public static native TemplateInstance post(Post post, LoggedUser user, long viewCount);

        public static native TemplateInstance toggle(Post post, LoggedUser user);
    }

    private final PostRepository postRepository;
    private final LoggedUser loggedUser;
    private final ViewRepository viewRepository;
    private final SessionIdProvider sessionIdProvider;

    @Inject
    public PostEndpoint(PostRepository postRepository,
                        LoggedUser loggedUser,
                        ViewRepository viewRepository,
                        SessionIdProvider sessionIdProvider) {
        this.postRepository = postRepository;
        this.loggedUser = loggedUser;
        this.viewRepository = viewRepository;
        this.sessionIdProvider = sessionIdProvider;
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response post(@PathParam("username") String username,
                         @PathParam("slug") String slug,
                         @Context HttpHeaders headers) {
        Post post = postRepository.findByUsernameAndSlug(username, slug)
                                  .orElseThrow(() -> new NotFoundException("Post not found! username=%s slug=$s".formatted(username, slug)));

        // Record view
        String sessionId = sessionIdProvider.getOrCreateSessionId(headers.getCookies().get(SessionIdProvider.VIEW_SESSION_COOKIE));
        viewRepository.recordView(post,
                                  loggedUser.isAuthenticated() ? loggedUser.getId() : null,
                                  sessionId,
                                  LocalDateTime.now());

        long viewCount = viewRepository.countByPost(post);

        TemplateInstance template = Templates.post(post, loggedUser, viewCount);
        ResponseBuilder response = Response.ok(template);
        if (headers.getCookies().get(SessionIdProvider.VIEW_SESSION_COOKIE) == null) {
            response.cookie(sessionIdProvider.createSessionCookie(sessionId));
        }
        return response.build();
    }

    @PUT
    @Logged
    @Path("/component/featured/toggle")
    @Transactional
    public Response toggleFeatured(@PathParam("username") String username,
                                   @PathParam("slug") String slug) {
        // ADMIN and EDITOR can select featured posts
        if (!loggedUser.isEditor()) {
            return Response.status(403)
                           .build();
        }

        var maybePost = postRepository.findByUsernameAndSlug(username, slug);
        if (maybePost.isEmpty()) {
            return Response.status(404).build();
        } else {
            var post = maybePost.get();
            post.setFeatured(!post.isFeatured());
            postRepository.save(post);
            return Response.ok()
                           .entity(Templates.toggle(post, loggedUser))
                           .build();
        }
    }
}