package dev.vepo.contraponto.tag;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.rss.RssFeedService;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/tags")
@ApplicationScoped
public class TagPageEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance edit(Tag tag, Links links, LoggedUser user, BreadcrumbTrail breadcrumb, SeoMetadata seo);

        public static native TemplateInstance grid(String tagSlug, Page<Post> posts);

        public static native TemplateInstance tag(Tag tag, Page<Post> posts, Links links, LoggedUser user, BreadcrumbTrail breadcrumb, SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private static final String TAG_NOT_FOUND_PREFIX = "Tag not found: ";

    public static String url(Tag tag) {
        return "/tags/%s".formatted(tag.getSlug());
    }

    private final TagRepository tagRepository;
    private final PostRepository postRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final RssFeedService rssFeedService;
    private final SeoService seoService;

    @Inject
    public TagPageEndpoint(TagRepository tagRepository,
                           PostRepository postRepository,
                           CustomPageRepository customPageRepository,
                           LoggedUser loggedUser,
                           BreadcrumbService breadcrumbService,
                           RssFeedService rssFeedService,
                           SeoService seoService) {
        this.tagRepository = tagRepository;
        this.postRepository = postRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.rssFeedService = rssFeedService;
        this.seoService = seoService;
    }

    @GET
    @Path("{slug}/edit")
    @Logged
    @Produces(MediaType.TEXT_HTML)
    public Response edit(@PathParam("slug") String slug) {
        if (!loggedUser.isEditor()) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        Tag tag = tagRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException(TAG_NOT_FOUND_PREFIX + slug));
        return Response.ok(Templates.edit(tag,
                                          customPageRepository.loadLinks(),
                                          loggedUser,
                                          breadcrumbService.reviewTagEdit(tag),
                                          seoService.forPrivatePage("Edit tag")))
                       .build();
    }

    @GET
    @Path("{slug}/components/grid")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance grid(@PathParam("slug") String slug,
                                 @QueryParam("limit") @DefaultValue("12") int limit,
                                 @QueryParam("page") int page) {
        Tag tag = tagRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException(TAG_NOT_FOUND_PREFIX + slug));
        return Templates.grid(tag.getSlug(),
                              postRepository.findPublishedByTagSlug(tag.getSlug(), PageQuery.forGrid(limit, page)));
    }

    @GET
    @Path("{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance tag(@PathParam("slug") String slug, @QueryParam("limit") @DefaultValue("12") int limit) {
        Tag tag = tagRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException(TAG_NOT_FOUND_PREFIX + slug));
        return Templates.tag(tag,
                             postRepository.findPublishedByTagSlug(tag.getSlug(), PageQuery.forGrid(limit, 1)),
                             customPageRepository.loadLinks(),
                             loggedUser,
                             breadcrumbService.forTag(tag),
                             seoService.forTag(tag));
    }

    @GET
    @Path("{slug}/feed")
    @Operation(hidden = true)
    @Produces("application/rss+xml;charset=UTF-8")
    public Response tagFeed(@PathParam("slug") String slug, @Context UriInfo uriInfo) {
        tagRepository.findBySlug(slug).orElseThrow(() -> new NotFoundException(TAG_NOT_FOUND_PREFIX + slug));
        return Response.ok(rssFeedService.tagFeed(slug, uriInfo.getBaseUri().toString())).build();
    }
}
