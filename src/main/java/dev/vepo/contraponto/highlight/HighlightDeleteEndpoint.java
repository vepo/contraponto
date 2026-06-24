package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/posts/{postId}/highlights/{highlightId}")
public class HighlightDeleteEndpoint {

    private final PostTextHighlightService highlightService;
    private final PostRepository postRepository;
    private final HighlightComponentEndpoint componentEndpoint;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightDeleteEndpoint(PostTextHighlightService highlightService,
                                   PostRepository postRepository,
                                   HighlightComponentEndpoint componentEndpoint,
                                   LoggedUser loggedUser) {
        this.highlightService = highlightService;
        this.postRepository = postRepository;
        this.componentEndpoint = componentEndpoint;
        this.loggedUser = loggedUser;
    }

    @DELETE
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response remove(@PathParam("postId") long postId, @PathParam("highlightId") long highlightId) {
        Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
        highlightService.remove(postId, highlightId, loggedUser.getId());
        return Toast.ok()
                    .i18nKey(I18nKeys.TOAST_HIGHLIGHT_REMOVED, I18nDefaults.HIGHLIGHT_REMOVED)
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .page(componentEndpoint.renderHighlights(post))
                    .build();
    }
}
