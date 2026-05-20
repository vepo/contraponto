package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/posts/{postId}/highlights")
public class HighlightCreateEndpoint {

    private final PostTextHighlightService highlightService;
    private final PostRepository postRepository;
    private final HighlightComponentEndpoint componentEndpoint;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightCreateEndpoint(PostTextHighlightService highlightService,
                                   PostRepository postRepository,
                                   HighlightComponentEndpoint componentEndpoint,
                                   LoggedUser loggedUser) {
        this.highlightService = highlightService;
        this.postRepository = postRepository;
        this.componentEndpoint = componentEndpoint;
        this.loggedUser = loggedUser;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response create(@PathParam("postId") long postId,
                           @FormParam("passage") String passage,
                           @FormParam("anchorJson") String anchorJson) {
        try {
            Post post = loadPost(postId);
            highlightService.create(postId, loggedUser.getId(), passage, anchorJson);
            return Toast.ok()
                        .i18nKey(I18nKeys.TOAST_HIGHLIGHT_CREATED, I18nDefaults.HIGHLIGHT_CREATED)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .page(componentEndpoint.renderHighlights(post))
                        .build();
        } catch (BadRequestException e) {
            return Toast.response(Status.BAD_REQUEST).message(e.getMessage()).type(Toast.Type.ERROR).build();
        } catch (NotFoundException _) {
            return Toast.response(Status.NOT_FOUND)
                        .i18nKey(I18nKeys.TOAST_POST_NOT_FOUND, I18nDefaults.POST_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

    private Post loadPost(long postId) {
        Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
        if (!post.isPublished()) {
            throw new NotFoundException("Post not found.");
        }
        return post;
    }
}
