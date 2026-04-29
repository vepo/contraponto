package dev.vepo.contraponto.components.forms;

import java.util.Optional;

import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
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
    private static final int TOAST_DURATION = 10_000;

    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public SaveDraftEndpoint(PostRepository postRepository, ImageRepository imageRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.loggedUser = loggedUser;
    }

    // ============================== PUBLIC API ==============================

    private Response buildErrorResponse(String message) {
        return Toast.ok() // Using OK status but with error type (original behavior)
                    .message(message)
                    .type(Toast.Type.ERROR)
                    .duration(TOAST_DURATION)
                    .build();
    }

    // ============================== VALIDATION ==============================

    private Response buildSuccessResponse(Post post) {
        return Toast.ok()
                    .message(SUCCESS_MSG_DRAFT_SAVED)
                    .type(Toast.Type.SUCCESS)
                    .duration(TOAST_DURATION)
                    .url("/write/draft/%d".formatted(post.getId()))
                    .build();
    }

    // ============================== POST RETRIEVAL / CREATION
    // ==============================

    private void fillPostMetadata(Post post, SaveDraftRequest request) {
        post.setSlug(request.slug());
        post.setAuthor(loggedUser.getUser());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setDescription(request.description());
        // Note: This endpoint does NOT set published or publishedAt – it's a draft.
    }

    // ============================== POST DATA MUTATION
    // ==============================

    private Post getOrCreatePost(SaveDraftRequest request) {
        if (request.id() != null) {
            return postRepository.findById(request.id()).orElseGet(Post::new);
        }
        return new Post();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(@BeanParam SaveDraftRequest request) {
        Optional<Response> validationError = validateRequest(request);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        Post post = getOrCreatePost(request);
        updateCoverImageIfProvided(post, request);
        setFormat(post, request);
        fillPostMetadata(post, request);
        postRepository.save(post);

        return buildSuccessResponse(post);
    }

    // ============================== RESPONSE BUILDING
    // ==============================

    private void setFormat(Post post, SaveDraftRequest request) {
        if (!isBlank(request.format())) {
            post.setFormat(Format.valueOf(request.format().toUpperCase()));
        } else {
            post.setFormat(Format.MARKDOWN);
        }
    }

    private void updateCoverImageIfProvided(Post post, SaveDraftRequest request) {
        if (request.coverId() != null && !request.coverId().isBlank()) {
            imageRepository.findByUuid(request.coverId())
                           .ifPresent(post::setCover);
        }
        // Note: Unlike publish endpoint, we do NOT clear cover if coverId is missing.
        // This allows preserving existing cover when saving a draft.
    }

    // ============================== UTILITIES ==============================

    private Optional<Response> validateRequest(SaveDraftRequest request) {
        if (isBlank(request.content())) {
            return Optional.of(buildErrorResponse(ERROR_MSG_CONTENT_REQUIRED));
        }
        if (isBlank(request.title())) {
            return Optional.of(buildErrorResponse(ERROR_MSG_TITLE_REQUIRED));
        }
        return Optional.empty();
    }
}