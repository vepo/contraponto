package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
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
@Path("/forms/blogs/{blogId}/subscribe")
public class BlogAudienceSubscribeEndpoint {

    private final BlogAudienceService audienceService;
    private final BlogRepository blogRepository;
    private final BlogAudienceComponentEndpoint componentEndpoint;
    private final LoggedUser loggedUser;

    @Inject
    public BlogAudienceSubscribeEndpoint(BlogAudienceService audienceService,
                                         BlogRepository blogRepository,
                                         BlogAudienceComponentEndpoint componentEndpoint,
                                         LoggedUser loggedUser) {
        this.audienceService = audienceService;
        this.blogRepository = blogRepository;
        this.componentEndpoint = componentEndpoint;
        this.loggedUser = loggedUser;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response toggle(@PathParam("blogId") long blogId) {
        try {
            boolean subscribed = audienceService.toggleEmailSubscribe(loggedUser.getId(), blogId);
            Blog blog = blogRepository.findById(blogId).orElseThrow(NotFoundException::new);
            String message = subscribed ? "You will receive new posts by email." : "Email subscription removed.";
            return Toast.ok()
                        .message(message)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .page(BlogAudienceComponentEndpoint.Templates.audienceControls(componentEndpoint.buildView(blog), true))
                        .build();
        } catch (BadRequestException e) {
            return Toast.response(Status.BAD_REQUEST).message(e.getMessage()).type(Toast.Type.ERROR).build();
        } catch (NotFoundException _) {
            return Toast.response(Status.NOT_FOUND).i18nKey(I18nKeys.TOAST_BLOG_NOT_FOUND_AUDIENCE, I18nDefaults.BLOG_NOT_FOUND_AUDIENCE).type(Toast.Type.ERROR)
                        .build();
        }
    }
}
