package dev.vepo.contraponto.components.forms;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.PostWriteService;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.git.GitSyncTrigger;
import dev.vepo.contraponto.git.PostGitSyncRequestedEvent;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.notification.BlogAudienceComponentEndpoint;
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.image.PostImageDependencyService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostPublicationService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.serie.SerieService;
import dev.vepo.contraponto.tag.TagSlug;
import dev.vepo.contraponto.tag.TagService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Logged
@ApplicationScoped
@Path("/forms/write/publish")
public class PublishEndpoint {

    // Constants for messages and durations
    private static final String ERROR_MSG_CONTENT_REQUIRED = "Content is required!";
    private static final String ERROR_MSG_TITLE_REQUIRED = "Title is required!";
    private static final String ERROR_MSG_INVALID_SLUG = "Slug can only contain lowercase letters, numbers, and hyphens";
    private static final String ERROR_MSG_SLUG_EXISTS = "Slug already exists!";
    private static final String ERROR_MSG_BLOG_REQUIRED = "Blog selection is required!";
    private static final String SUCCESS_MSG_PUBLISHED = "Post published!";
    private static final int TOAST_DURATION_SHORT = 10_000;
    private static final int TOAST_DURATION_LONG = 10_000;

    private static final Pattern SLUG_GENERATION_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-]");

    private final PostRepository postRepository;
    private final PostPublicationService publicationService;
    private final PostWriteService postWriteService;
    private final CustomPageRepository customPageRepository;
    private final ImageRepository imageRepository;
    private final PostImageDependencyService postImageDependencyService;
    private final TagService tagService;
    private final SerieService serieService;
    private final LoggedUser loggedUser;
    private final Event<PostGitSyncRequestedEvent> postGitSyncEvents;
    private final BlogAudienceComponentEndpoint audienceComponentEndpoint;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public PublishEndpoint(PostRepository postRepository,
                           PostPublicationService publicationService,
                           PostWriteService postWriteService,
                           CustomPageRepository customPageRepository,
                           ImageRepository imageRepository,
                           PostImageDependencyService postImageDependencyService,
                           TagService tagService,
                           SerieService serieService,
                           Event<PostGitSyncRequestedEvent> postGitSyncEvents,
                           BlogAudienceComponentEndpoint audienceComponentEndpoint,
                           LoggedUser loggedUser,
                           BreadcrumbService breadcrumbService) {
        this.postRepository = postRepository;
        this.publicationService = publicationService;
        this.postWriteService = postWriteService;
        this.customPageRepository = customPageRepository;
        this.imageRepository = imageRepository;
        this.postImageDependencyService = postImageDependencyService;
        this.tagService = tagService;
        this.serieService = serieService;
        this.postGitSyncEvents = postGitSyncEvents;
        this.audienceComponentEndpoint = audienceComponentEndpoint;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
    }

    // ============================== PUBLIC API ==============================

    private Response buildErrorResponse(String i18nKey, String ptBrMessage) {
        return Toast.response(Status.BAD_REQUEST)
                    .i18nKey(i18nKey, ptBrMessage)
                    .type(Toast.Type.ERROR)
                    .duration(TOAST_DURATION_SHORT)
                    .build();
    }

    private Response buildSuccessResponse(PublishedPostView view) {
        Post post = view.post();
        Blog blog = post.getBlog();
        String username = blog.getOwner().getUsername();
        String slug = post.getSlug();

        String postUrl;
        if (blog.isMain()) {
            postUrl = "/" + username + "/post/" + slug;
        } else {
            postUrl = "/" + username + "/" + blog.getSlug() + "/post/" + slug;
        }

        Links links = blog.isMain() ? customPageRepository.loadLinks() : customPageRepository.loadLinks(blog.getId());

        return Toast.ok()
                    .i18nKey(I18nKeys.TOAST_POST_PUBLISHED, I18nDefaults.POST_PUBLISHED)
                    .type(Toast.Type.SUCCESS)
                    .duration(TOAST_DURATION_LONG)
                    .url(postUrl)
                    .page(PostEndpoint.Templates.post(view,
                                                      seriePostsFor(post),
                                                      links,
                                                      loggedUser,
                                                      0L,
                                                      0L,
                                                      audienceComponentEndpoint.buildView(blog),
                                                      breadcrumbService.forPost(view)))
                    .build();
    }

    private void fillPostMetadata(Post post, SaveDraftRequest request) {
        post.setTitle(request.title());
        postImageDependencyService.normalizeAndStoreContent(post, request.content());
        post.setDescription(request.description());
    }

    private String generateSlugFromTitle(String title) {
        return title.toLowerCase()
                    .replaceAll(SLUG_GENERATION_PATTERN.pattern(), "-");
    }

    private void generateSlugIfMissing(Post post, SaveDraftRequest request) {
        if (isBlank(request.slug())) {
            post.setSlug(generateSlugFromTitle(request.title()));
        } else {
            post.setSlug(request.slug());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response publish(@BeanParam SaveDraftRequest request) {
        var validationError = validateRequest(request);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        Blog blog = postWriteService.requireEditableBlog(request.blogId(), loggedUser);
        Post post = postWriteService.resolvePostForWrite(request.id(), blog, loggedUser);
        setFormatIfProvided(post, request);
        updateCoverImage(post, request, blog);
        fillPostMetadata(post, request);
        generateSlugIfMissing(post, request);
        serieService.applySerieTitleToPost(post, request.serieTitle());
        postRepository.save(post);
        tagService.syncPostTags(post, request.tagsJson());
        postImageDependencyService.syncPostDependencies(post);
        PostPublication published = publicationService.publish(post);

        postGitSyncEvents.fire(new PostGitSyncRequestedEvent(post.getId(), GitSyncTrigger.PUBLISH));

        Post rendered = postRepository.findByIdWithTags(post.getId()).orElse(post);
        return buildSuccessResponse(new PublishedPostView(rendered, published));
    }

    private List<Post> seriePostsFor(Post post) {
        if (post.getSerie() == null) {
            return Collections.emptyList();
        }
        return postRepository.findPublishedBySerieOrdered(post.getSerie().getId());
    }

    private void setFormatIfProvided(Post post, SaveDraftRequest request) {
        if (!isBlank(request.format())) {
            post.setFormat(Format.valueOf(request.format().toUpperCase()));
        } else {
            post.setFormat(Format.MARKDOWN);
        }
    }

    private boolean slugAlreadyExistsForDifferentPost(SaveDraftRequest request, Long blogId) {
        if (isBlank(request.slug())) {
            return false;
        }
        return postRepository.slugExists(blogId, request.slug(), request.id());
    }

    private void updateCoverImage(Post post, SaveDraftRequest request, Blog blog) {
        if (request.coverId() != null && !request.coverId().isBlank()) {
            imageRepository.findByUuidAndBlogId(request.coverId(), blog.getId()).ifPresent(post::setCover);
        } else if (post.getCover() != null) {
            post.setCover(null);
        }
    }

    private Optional<Response> validateRequest(SaveDraftRequest request) {
        if (request.blogId() == null) {
            return Optional.of(buildErrorResponse(I18nKeys.TOAST_POST_BLOG_REQUIRED, I18nDefaults.POST_BLOG_REQUIRED));
        }
        if (isBlank(request.content())) {
            return Optional.of(buildErrorResponse(I18nKeys.TOAST_POST_CONTENT_REQUIRED, I18nDefaults.POST_CONTENT_REQUIRED));
        }
        if (isBlank(request.title())) {
            return Optional.of(buildErrorResponse(I18nKeys.TOAST_POST_TITLE_REQUIRED, I18nDefaults.POST_TITLE_REQUIRED));
        }
        if (TagSlug.hasInvalidSlugCharacters(request.slug())) {
            return Optional.of(buildErrorResponse(I18nKeys.TOAST_POST_INVALID_SLUG, I18nDefaults.POST_INVALID_SLUG));
        }
        if (slugAlreadyExistsForDifferentPost(request, request.blogId())) {
            return Optional.of(buildErrorResponse(I18nKeys.TOAST_POST_SLUG_EXISTS, I18nDefaults.POST_SLUG_EXISTS));
        }
        return Optional.empty();
    }
}