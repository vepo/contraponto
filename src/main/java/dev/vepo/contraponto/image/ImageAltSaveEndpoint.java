package dev.vepo.contraponto.image;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogAccess;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/blogs/{blogId}/images/{uuid}/alt")
public class ImageAltSaveEndpoint {

    private static final String SUCCESS_MSG = "Image updated.";

    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;
    private final ImageControlService imageControlService;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public ImageAltSaveEndpoint(BlogRepository blogRepository,
                                BlogAccess blogAccess,
                                ImageControlService imageControlService,
                                CustomPageRepository customPageRepository,
                                LoggedUser loggedUser) {
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
        this.imageControlService = imageControlService;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    @PUT
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveAlt(@PathParam("blogId") long blogId,
                            @PathParam("uuid") String uuid,
                            @FormParam("altText") String altText,
                            @FormParam("page") @jakarta.ws.rs.DefaultValue("1") int page) {
        Blog blog = blogRepository.findById(blogId).orElse(null);
        if (blog == null || !blogAccess.canEdit(blog, loggedUser)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            imageControlService.updateAltText(blog, uuid, altText);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var links = blog.isMain() ? customPageRepository.loadLinks() : customPageRepository.loadLinks(blog.getId());
        var images = imageControlService.listForBlog(blog, PageQuery.forGrid(20, page));
        return Toast.ok()
                    .message(SUCCESS_MSG)
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .page(ImageControlEndpoint.Templates.list(blog, images, links, loggedUser))
                    .build();
    }
}
