package dev.vepo.contraponto.highlight;

import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
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
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/posts/{postId}/highlight-proposals/{proposalId}")
public class HighlightProposalModerationEndpoint {

    private final PostTextHighlightService highlightService;
    private final PostRepository postRepository;
    private final HighlightComponentEndpoint componentEndpoint;
    private final NavigationHubService navigationHubService;
    private final LoggedUser loggedUser;

    @Inject
    public HighlightProposalModerationEndpoint(PostTextHighlightService highlightService,
                                               PostRepository postRepository,
                                               HighlightComponentEndpoint componentEndpoint,
                                               NavigationHubService navigationHubService,
                                               LoggedUser loggedUser) {
        this.highlightService = highlightService;
        this.postRepository = postRepository;
        this.componentEndpoint = componentEndpoint;
        this.navigationHubService = navigationHubService;
        this.loggedUser = loggedUser;
    }

    @POST
    @Path("approve")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response approve(@PathParam("postId") long postId,
                            @PathParam("proposalId") long proposalId,
                            @QueryParam("from") String from) {
        return moderate(postId, proposalId, true, from);
    }

    private Response moderate(long postId, long proposalId, boolean approve, String from) {
        try {
            Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
            if (approve) {
                highlightService.approveProposal(postId, proposalId, loggedUser.getId());
            } else {
                highlightService.rejectProposal(postId, proposalId, loggedUser.getId());
            }
            var builder = Toast.ok()
                               .i18nKey(approve ? I18nKeys.TOAST_HIGHLIGHT_OFFICIAL_APPROVED
                                                : I18nKeys.TOAST_HIGHLIGHT_PROPOSAL_REJECTED,
                                        approve ? I18nDefaults.HIGHLIGHT_OFFICIAL_APPROVED
                                                : I18nDefaults.HIGHLIGHT_PROPOSAL_REJECTED)
                               .type(Toast.Type.SUCCESS)
                               .duration(Toast.TOAST_DEFAULT_DURATION_MS);
            if ("writing".equals(from)) {
                return builder.page(navigationHubService.shell(NavigationHub.WRITING, "highlights", 1)).build();
            }
            return builder.page(componentEndpoint.renderHighlights(post)).build();
        } catch (ForbiddenException e) {
            return Toast.response(Status.FORBIDDEN).message(e.getMessage()).type(Toast.Type.ERROR).build();
        } catch (NotFoundException _) {
            return Toast.response(Status.NOT_FOUND).i18nKey(I18nKeys.TOAST_POST_NOT_FOUND, I18nDefaults.POST_NOT_FOUND)
                        .type(Toast.Type.ERROR).build();
        }
    }

    @POST
    @Path("reject")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response reject(@PathParam("postId") long postId,
                           @PathParam("proposalId") long proposalId,
                           @QueryParam("from") String from) {
        return moderate(postId, proposalId, false, from);
    }
}
