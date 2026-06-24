package dev.vepo.contraponto.readinglist;

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
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.container.ContainerRequestContext;

@Logged
@ApplicationScoped
@Path("/forms/posts/{postId}/reading-list")
public class ReadingListSaveEndpoint {

    private final ReadingListService readingListService;
    private final PostRepository postRepository;
    private final ReadingListMutationResponse mutationResponse;
    private final LoggedUser loggedUser;

    @Inject
    public ReadingListSaveEndpoint(ReadingListService readingListService,
                                   PostRepository postRepository,
                                   ReadingListMutationResponse mutationResponse,
                                   LoggedUser loggedUser) {
        this.readingListService = readingListService;
        this.postRepository = postRepository;
        this.mutationResponse = mutationResponse;
        this.loggedUser = loggedUser;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response save(@PathParam("postId") long postId,
                         @QueryParam("tab") String tab,
                         @QueryParam("page") @DefaultValue("1") int page,
                         @Context ContainerRequestContext requestContext) {
        try {
            var result = readingListService.save(loggedUser.getId(), postId);
            Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
            var toast = Toast.ok().type(Toast.Type.SUCCESS).duration(Toast.TOAST_DEFAULT_DURATION_MS);
            toast = switch (result.type()) {
                case CREATED, REQUEUED -> toast.i18nKey(I18nKeys.TOAST_READING_LIST_SAVED, I18nDefaults.READING_LIST_SAVED);
                case ALREADY_SAVED -> toast.i18nKey(I18nKeys.TOAST_READING_LIST_ALREADY_SAVED, I18nDefaults.READING_LIST_ALREADY_SAVED);
            };
            return mutationResponse.build(toast, post, tab, page, requestContext);
        } catch (BadRequestException e) {
            if ("Reading list limit reached.".equals(e.getMessage())) {
                return Toast.response(Status.BAD_REQUEST)
                            .i18nKey(I18nKeys.TOAST_READING_LIST_LIMIT_REACHED, I18nDefaults.READING_LIST_LIMIT_REACHED)
                            .type(Toast.Type.ERROR)
                            .build();
            }
            return Toast.response(Status.BAD_REQUEST).message(e.getMessage()).type(Toast.Type.ERROR).build();
        } catch (NotFoundException _) {
            return Toast.response(Status.NOT_FOUND)
                        .i18nKey(I18nKeys.TOAST_POST_NOT_FOUND, I18nDefaults.POST_NOT_FOUND)
                        .type(Toast.Type.ERROR)
                        .build();
        }
    }

}
