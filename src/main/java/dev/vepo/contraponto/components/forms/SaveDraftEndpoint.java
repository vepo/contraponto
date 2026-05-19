package dev.vepo.contraponto.components.forms;

import java.util.Optional;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.git.GitSyncTrigger;
import dev.vepo.contraponto.git.PostGitSyncRequestedEvent;
import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.post.PostWriteService;
import dev.vepo.contraponto.image.PostImageDependencyService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.serie.SerieService;
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

@Logged
@ApplicationScoped
@Path("/forms/write/draft")
public class SaveDraftEndpoint {

    // Constants for messages and durations
    private static final String ERROR_MSG_CONTENT_REQUIRED = "Content is required!";
    private static final String ERROR_MSG_TITLE_REQUIRED = "Title is required!";
    private static final String SUCCESS_MSG_DRAFT_SAVED = "Draft saved successfully!";

    private final PostRepository postRepository;
    private final PostWriteService postWriteService;
    private final ImageRepository imageRepository;
    private final PostImageDependencyService postImageDependencyService;
    private final TagService tagService;
    private final SerieService serieService;
    private final LoggedUser loggedUser;
    private final Event<PostGitSyncRequestedEvent> postGitSyncEvents;

    @Inject
    public SaveDraftEndpoint(PostRepository postRepository,
                             PostWriteService postWriteService,
                             ImageRepository imageRepository,
                             PostImageDependencyService postImageDependencyService,
                             TagService tagService,
                             SerieService serieService,
                             Event<PostGitSyncRequestedEvent> postGitSyncEvents,
                             LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.postWriteService = postWriteService;
        this.imageRepository = imageRepository;
        this.postImageDependencyService = postImageDependencyService;
        this.tagService = tagService;
        this.serieService = serieService;
        this.postGitSyncEvents = postGitSyncEvents;
        this.loggedUser = loggedUser;
    }

    // ============================== PUBLIC API ==============================

    private Response buildErrorResponse(String i18nKey) {
        return Toast.ok() // Using OK status but with error type (original behavior)
                    .i18nKey(i18nKey)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Response buildSuccessResponse(Post post) {
        return Toast.ok()
                    .i18nKey(I18nKeys.TOAST_DRAFT_SAVED, I18nDefaults.DRAFT_SAVED)
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .url("/write/draft/%d".formatted(post.getId()))
                    .build();
    }

    private void fillPostMetadata(Post post, SaveDraftRequest request) {
        post.setSlug(request.slug());
        post.setTitle(request.title());
        postImageDependencyService.normalizeAndStoreContent(post, request.content());
        post.setDescription(request.description());
        // Note: This endpoint does NOT set published or publishedAt – it's a draft.
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(@BeanParam SaveDraftRequest request) {
        var validationError = validateRequest(request);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        Blog blog = postWriteService.requireEditableBlog(request.blogId(), loggedUser);
        Post post = postWriteService.resolvePostForWrite(request.id(), blog, loggedUser);
        updateCoverImageIfProvided(post, request, blog);
        setFormat(post, request);
        fillPostMetadata(post, request);
        serieService.applySerieTitleToPost(post, request.serieTitle());
        postRepository.save(post);
        tagService.syncPostTags(post, request.tagsJson());
        postImageDependencyService.syncPostDependencies(post);

        postGitSyncEvents.fire(new PostGitSyncRequestedEvent(post.getId(), GitSyncTrigger.DRAFT_SAVE));

        return buildSuccessResponse(post);
    }

    private void setFormat(Post post, SaveDraftRequest request) {
        if (!isBlank(request.format())) {
            post.setFormat(Format.valueOf(request.format().toUpperCase()));
        } else {
            post.setFormat(Format.MARKDOWN);
        }
    }

    private void updateCoverImageIfProvided(Post post, SaveDraftRequest request, Blog blog) {
        if (request.coverId() != null && !request.coverId().isBlank()) {
            imageRepository.findByUuidAndBlogId(request.coverId(), blog.getId())
                           .ifPresent(post::setCover);
        }
        // Note: Unlike publish endpoint, we do NOT clear cover if coverId is missing.
        // This allows preserving existing cover when saving a draft.
    }

    // ============================== UTILITIES ==============================

    private Optional<Response> validateRequest(SaveDraftRequest request) {
        if (isBlank(request.content())) {
            return Optional.of(buildErrorResponse(I18nKeys.TOAST_POST_CONTENT_REQUIRED));
        }
        if (isBlank(request.title())) {
            return Optional.of(buildErrorResponse(I18nKeys.TOAST_POST_TITLE_REQUIRED));
        }
        return Optional.empty();
    }
}