package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.List;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
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

@Path("{username}")
@ApplicationScoped
public class PostEndpoint {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance history(List<PostChangeDiffService.VersionDiff> versions);

        public static native TemplateInstance post(PublishedPostView view,
                                                   Links links,
                                                   LoggedUser user,
                                                   long viewCount,
                                                   List<PostChangeDiffService.VersionDiff> versions);

        public static native TemplateInstance toggle(Post post, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    public static String extractUrl(Post post) {
        var blog = post.getBlog();
        if (post.getBlog().isMain()) {
            return "/%s/post/%s".formatted(blog.getOwner().getUsername(), post.getSlug());
        } else {
            return "/%s/%s/post/%s".formatted(blog.getOwner().getUsername(), blog.getSlug(), post.getSlug());
        }
    }

    private final PostRepository postRepository;
    private final PostPublicationRepository publicationRepository;
    private final PostChangeDiffService changeDiffService;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final ViewRepository viewRepository;

    private final SessionIdProvider sessionIdProvider;

    @Inject
    public PostEndpoint(PostRepository postRepository,
                        PostPublicationRepository publicationRepository,
                        PostChangeDiffService changeDiffService,
                        CustomPageRepository customPageRepository,
                        LoggedUser loggedUser,
                        ViewRepository viewRepository,
                        SessionIdProvider sessionIdProvider) {
        this.postRepository = postRepository;
        this.publicationRepository = publicationRepository;
        this.changeDiffService = changeDiffService;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.viewRepository = viewRepository;
        this.sessionIdProvider = sessionIdProvider;
    }

    @GET
    @Path("{blogSlug}/post/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response blogPost(@PathParam("username") String username,
                             @PathParam("blogSlug") String blogSlug,
                             @PathParam("slug") String slug,
                             @Context HttpHeaders headers) {
        return renderPost(postRepository.findBlogPost(username, blogSlug, slug)
                                        .orElseThrow(() -> new NotFoundException("Post not found! username=%s slug=%s".formatted(username, slug))),
                          headers);
    }

    @GET
    @Path("{blogSlug}/post/{slug}/components/history")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogPostHistory(@PathParam("username") String username,
                                            @PathParam("blogSlug") String blogSlug,
                                            @PathParam("slug") String slug) {
        return historyFor(postRepository.findBlogPost(username, blogSlug, slug));
    }

    private TemplateInstance historyFor(Optional<Post> maybePost) {
        Post post = maybePost.orElseThrow(() -> new NotFoundException("Post not found"));
        List<PostPublication> publications = publicationRepository.findByPostIdOrderByVersionDesc(post.getId());
        return Templates.history(changeDiffService.buildVersionDiffs(publications));
    }

    private Links loadLinks(Post post) {
        if (post.getBlog().isMain()) {
            return customPageRepository.loadLinks();
        } else {
            return customPageRepository.loadLinks(post.getBlog().getId());
        }
    }

    @GET
    @Path("post/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response mainBlogPost(@PathParam("username") String username,
                                 @PathParam("slug") String slug,
                                 @Context HttpHeaders headers) {
        return renderPost(postRepository.findMainBlogPost(username, slug)
                                        .orElseThrow(() -> new NotFoundException("Post not found! username=%s slug=%s".formatted(username, slug))),
                          headers);
    }

    @GET
    @Path("post/{slug}/components/history")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mainBlogPostHistory(@PathParam("username") String username,
                                                @PathParam("slug") String slug) {
        return historyFor(postRepository.findMainBlogPost(username, slug));
    }

    private Response renderPost(Post post, HttpHeaders headers) {
        // Record view
        String sessionId = sessionIdProvider.getOrCreateSessionId(headers.getCookies().get(SessionIdProvider.VIEW_SESSION_COOKIE));
        viewRepository.recordView(post,
                                  loggedUser.isAuthenticated() ? loggedUser.getId() : null,
                                  sessionId,
                                  LocalDateTime.now());

        long viewCount = viewRepository.countByPost(post);

        PublishedPostView view = new PublishedPostView(post, post.getLivePublication());
        var versions = changeDiffService.buildVersionDiffs(publicationRepository.findByPostIdOrderByVersionDesc(post.getId()));
        TemplateInstance template = Templates.post(view, loadLinks(post), loggedUser, viewCount, versions);
        ResponseBuilder response = Response.ok(template);
        if (headers.getCookies().get(SessionIdProvider.VIEW_SESSION_COOKIE) == null) {
            response.cookie(sessionIdProvider.createSessionCookie(sessionId));
        }
        return response.build();
    }

    @PUT
    @Logged
    @Path("{blogSlug}/post/{slug}/component/featured/toggle")
    @Transactional
    public Response toggleBlogPostFeatured(@PathParam("username") String username,
                                           @PathParam("blogSlug") String blogSlug,
                                           @PathParam("slug") String slug) {
        // ADMIN and EDITOR can select featured posts
        if (!loggedUser.isEditor()) {
            return Response.status(403)
                           .build();
        }

        return togglePostFeatured(postRepository.findBlogPost(username, blogSlug, slug));
    }

    @PUT
    @Logged
    @Path("post/{slug}/component/featured/toggle")
    @Transactional
    public Response toggleMapPostFeatured(@PathParam("username") String username,
                                          @PathParam("slug") String slug) {
        // ADMIN and EDITOR can select featured posts
        if (!loggedUser.isEditor()) {
            return Response.status(403)
                           .build();
        }

        return togglePostFeatured(postRepository.findMainBlogPost(username, slug));
    }

    public Response togglePostFeatured(Optional<Post> maybePost) {
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