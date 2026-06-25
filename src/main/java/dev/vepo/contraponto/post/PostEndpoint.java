package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogPublicUrlService;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.share.ShareLinks;
import dev.vepo.contraponto.shared.share.ShareView;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.notification.BlogAudienceComponentEndpoint;
import dev.vepo.contraponto.notification.BlogAudienceView;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.readinglist.ReadingListActionView;
import dev.vepo.contraponto.readinglist.ReadingListService;
import dev.vepo.contraponto.readingtime.ReadingTimeRepository;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
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
import jakarta.ws.rs.core.UriBuilder;

@Path("{username}")
@ApplicationScoped
public class PostEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance history(List<PostChangeDiffService.VersionDiff> versions);

        public static native TemplateInstance historyModal(List<PostChangeDiffService.VersionDiff> versions);

        public static native TemplateInstance post(PublishedPostView view,
                                                   List<Post> seriePosts,
                                                   List<Post> relatedPosts,
                                                   Links links,
                                                   LoggedUser user,
                                                   long viewCount,
                                                   long averageReadingSeconds,
                                                   BlogAudienceView audience,
                                                   ShareView share,
                                                   ReadingListActionView readingListView,
                                                   BreadcrumbTrail breadcrumb,
                                                   SeoMetadata seo);

        public static native TemplateInstance toggle(Post post, LoggedUser user);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private static final int RELATED_POST_LIMIT = 4;

    private final PostRepository postRepository;
    private final PostPublicationRepository publicationRepository;
    private final PostChangeDiffService changeDiffService;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final ViewRepository viewRepository;
    private final ReadingTimeRepository readingTimeRepository;
    private final PostEngagementService postEngagementService;

    private final SessionIdProvider sessionIdProvider;
    private final BlogAudienceComponentEndpoint audienceComponentEndpoint;
    private final BreadcrumbService breadcrumbService;
    private final SeoService seoService;
    private final BlogRepository blogRepository;
    private final PostSlugAliasRepository postSlugAliasRepository;
    private final ReadingListService readingListService;
    private final BlogPublicUrlService blogPublicUrlService;

    @Inject
    public PostEndpoint(PostRepository postRepository,
                        PostPublicationRepository publicationRepository,
                        PostChangeDiffService changeDiffService,
                        CustomPageRepository customPageRepository,
                        LoggedUser loggedUser,
                        ViewRepository viewRepository,
                        ReadingTimeRepository readingTimeRepository,
                        PostEngagementService postEngagementService,
                        SessionIdProvider sessionIdProvider,
                        BlogAudienceComponentEndpoint audienceComponentEndpoint,
                        BreadcrumbService breadcrumbService,
                        SeoService seoService,
                        BlogRepository blogRepository,
                        PostSlugAliasRepository postSlugAliasRepository,
                        ReadingListService readingListService,
                        BlogPublicUrlService blogPublicUrlService) {
        this.postRepository = postRepository;
        this.publicationRepository = publicationRepository;
        this.changeDiffService = changeDiffService;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.viewRepository = viewRepository;
        this.readingTimeRepository = readingTimeRepository;
        this.postEngagementService = postEngagementService;
        this.sessionIdProvider = sessionIdProvider;
        this.audienceComponentEndpoint = audienceComponentEndpoint;
        this.breadcrumbService = breadcrumbService;
        this.seoService = seoService;
        this.blogRepository = blogRepository;
        this.postSlugAliasRepository = postSlugAliasRepository;
        this.readingListService = readingListService;
        this.blogPublicUrlService = blogPublicUrlService;
    }

    @GET
    @Path("{blogSlug}/post/{slug}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public Response blogPost(@PathParam("username") String username,
                             @PathParam("blogSlug") String blogSlug,
                             @PathParam("slug") String slug,
                             @Context HttpHeaders headers) {
        return resolveSecondaryBlogPost(username, blogSlug, slug, headers);
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

    @GET
    @Path("{blogSlug}/post/{slug}/components/history/modal")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogPostHistoryModal(@PathParam("username") String username,
                                                 @PathParam("blogSlug") String blogSlug,
                                                 @PathParam("slug") String slug) {
        return historyModalFor(postRepository.findBlogPost(username, blogSlug, slug));
    }

    private TemplateInstance historyFor(Optional<Post> maybePost) {
        Post post = maybePost.orElseThrow(() -> new NotFoundException("Post not found"));
        return Templates.history(versionDiffsFor(post));
    }

    private TemplateInstance historyModalFor(Optional<Post> maybePost) {
        Post post = maybePost.orElseThrow(() -> new NotFoundException("Post not found"));
        return Templates.historyModal(versionDiffsFor(post));
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
        return resolveMainBlogPost(username, slug, headers);
    }

    @GET
    @Path("post/{slug}/components/history")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mainBlogPostHistory(@PathParam("username") String username,
                                                @PathParam("slug") String slug) {
        return historyFor(postRepository.findMainBlogPost(username, slug));
    }

    @GET
    @Path("post/{slug}/components/history/modal")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mainBlogPostHistoryModal(@PathParam("username") String username,
                                                     @PathParam("slug") String slug) {
        return historyModalFor(postRepository.findMainBlogPost(username, slug));
    }

    private Response redirectToPost(Post post) {
        return Response.status(Response.Status.MOVED_PERMANENTLY)
                       .location(UriBuilder.fromPath(PostPaths.extractUrl(post)).build())
                       .build();
    }

    private Optional<Response> redirectViaAlias(Blog blog, String slug) {
        return postSlugAliasRepository.findPostIdByBlogAndSlug(blog.getId(), slug)
                                      .flatMap(postRepository::findPublishedWithBlogForRedirect)
                                      .map(this::redirectToPost);
    }

    private List<Post> relatedPostsFor(Post post) {
        return postRepository.findRelatedPublishedBySharedTags(post, RELATED_POST_LIMIT);
    }

    private Response renderPost(Post post, HttpHeaders headers) {
        String sessionId = sessionIdProvider.getOrCreateSessionId(headers.getCookies().get(SessionIdProvider.VIEW_SESSION_COOKIE));
        Long viewerUserId = loggedUser.isAuthenticated() ? loggedUser.getId() : null;
        if (postEngagementService.shouldRecordReaderEngagement(post, viewerUserId)) {
            viewRepository.recordView(post, viewerUserId, sessionId, LocalDateTime.now(ZoneId.systemDefault()));
        }

        long viewCount = viewRepository.countByPost(post);
        long averageReadingSeconds = readingTimeRepository.averageSecondsByPost(post);

        PostPublication live = post.getLivePublication();
        PublishedPostView view = new PublishedPostView(post, live);
        BlogAudienceView audience = audienceComponentEndpoint.buildView(post.getBlog());
        ReadingListActionView readingListView = readingListService.buildActionView(post, viewerUserId);
        BreadcrumbTrail breadcrumb = breadcrumbService.forPost(view);
        ShareView share = ShareLinks.from(PostTemplateExtensions.liveTitle(view),
                                          blogPublicUrlService.canonicalOrPlatformAbsolute(post));
        TemplateInstance template = Templates.post(view,
                                                   seriePostsFor(post),
                                                   relatedPostsFor(post),
                                                   loadLinks(post),
                                                   loggedUser,
                                                   viewCount,
                                                   averageReadingSeconds,
                                                   audience,
                                                   share,
                                                   readingListView,
                                                   breadcrumb,
                                                   seoService.forPost(view, breadcrumb));
        ResponseBuilder response = Response.ok(template);
        if (headers.getCookies().get(SessionIdProvider.VIEW_SESSION_COOKIE) == null) {
            response.cookie(sessionIdProvider.createSessionCookie(sessionId));
        }
        return response.build();
    }

    private Response resolveMainBlogPost(String username, String slug, HttpHeaders headers) {
        var post = postRepository.findMainBlogPost(username, slug);
        return post.map(p -> renderPost(p, headers))
                   .orElseGet(() -> blogRepository.findMainByOwnerUsername(username)
                                                  .flatMap(blog -> redirectViaAlias(blog, slug))
                                                  .orElseThrow(() -> new NotFoundException("Post not found! username=%s slug=%s".formatted(username, slug))));
    }

    private Response resolveSecondaryBlogPost(String username, String blogSlug, String slug, HttpHeaders headers) {
        var post = postRepository.findBlogPost(username, blogSlug, slug);
        return post.map(p -> renderPost(p, headers))
                   .orElseGet(() -> blogRepository.findActiveByOwnerUsernameAndSlug(username, blogSlug)
                                                  .flatMap(blog -> redirectViaAlias(blog, slug))
                                                  .orElseThrow(() -> new NotFoundException("Post not found! username=%s slug=%s".formatted(username, slug))));
    }

    private List<Post> seriePostsFor(Post post) {
        if (post.getSerie() == null) {
            return Collections.emptyList();
        }
        return postRepository.findPublishedBySerieOrdered(post.getSerie().getId());
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

    private List<PostChangeDiffService.VersionDiff> versionDiffsFor(Post post) {
        List<PostPublication> publications = publicationRepository.findByPostIdOrderByVersionDesc(post.getId());
        return changeDiffService.buildVersionDiffs(publications);
    }
}