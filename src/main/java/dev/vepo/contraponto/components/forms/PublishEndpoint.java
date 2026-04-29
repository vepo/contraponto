package dev.vepo.contraponto.components.forms;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
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
    private static final String SUCCESS_MSG_PUBLISHED = "Post published!";
    private static final int TOAST_DURATION_SHORT = 10_000;
    private static final int TOAST_DURATION_LONG = 10_000; // Same as short, kept for clarity

    // Slug validation pattern (lowercase letters, digits, hyphens)
    private static final Pattern INVALID_SLUG_CHARS = Pattern.compile("[^a-z0-9\\-]");
    // Slug generation pattern (any letters/digits/hyphens replaced with hyphen)
    private static final Pattern SLUG_GENERATION_PATTERN = Pattern.compile("[^a-zA-Z0-9\\-]");

    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public PublishEndpoint(PostRepository postRepository, ImageRepository imageRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.loggedUser = loggedUser;
    }

    // ============================== PUBLIC API ==============================

    private Response buildErrorResponse(String message) {
        return Toast.response(Status.BAD_REQUEST)
                    .message(message)
                    .type(Toast.Type.ERROR)
                    .duration(TOAST_DURATION_SHORT)
                    .build();
    }

    private Response buildSuccessResponse(Post post) {
        String postUrl = "/%s/post/%s".formatted(post.getAuthor().getUsername(), post.getSlug());
        return Toast.ok()
                    .message(SUCCESS_MSG_PUBLISHED)
                    .type(Toast.Type.SUCCESS)
                    .duration(TOAST_DURATION_LONG)
                    .url(postUrl)
                    .page(PostEndpoint.Templates.post(post, loggedUser, 0L))
                    .build();
    }

    private void fillPostMetadata(Post post, SaveDraftRequest request) {
        post.setAuthor(loggedUser.getUser());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setDescription(request.description());
        // slug is handled separately
    }

    /**
     * Converts a title into a URL‑friendly slug. Anything not a letter, digit or
     * hyphen is replaced with a hyphen.
     */
    private String generateSlugFromTitle(String title) {
        return title.toLowerCase()
                    .replaceAll(SLUG_GENERATION_PATTERN.pattern(), "-");
    }

    private void generateSlugIfMissing(Post post, SaveDraftRequest request) {
        if (isBlank(request.slug())) {
            String generatedSlug = generateSlugFromTitle(request.title());
            post.setSlug(generatedSlug);
        } else {
            post.setSlug(request.slug());
        }
    }

    private Post getOrCreatePost(SaveDraftRequest request) {
        if (request.id() != null) {
            return postRepository.findById(request.id()).orElseGet(Post::new);
        }
        return new Post();
    }

    private boolean hasInvalidSlugCharacters(String slug) {
        return slug != null && INVALID_SLUG_CHARS.matcher(slug).find();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void markAsPublishedIfNeeded(Post post) {
        if (!post.isPublished()) {
            post.setPublished(true);
            post.setPublishedAt(LocalDateTime.now());
        }
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response publish(@BeanParam SaveDraftRequest request) {
        Optional<Response> validationError = validateRequest(request);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        Post post = getOrCreatePost(request);
        setFormatIfProvided(post, request);
        updateCoverImage(post, request);
        fillPostMetadata(post, request);
        generateSlugIfMissing(post, request);
        markAsPublishedIfNeeded(post);
        postRepository.save(post);

        return buildSuccessResponse(post);
    }

    private void setFormatIfProvided(Post post, SaveDraftRequest request) {
        if (!isBlank(request.format())) {
            post.setFormat(Format.valueOf(request.format().toUpperCase()));
        } else {
            post.setFormat(Format.MARKDOWN); // sensible default
        }
    }

    private boolean slugAlreadyExistsForDifferentPost(SaveDraftRequest request) {
        // If no slug provided, no conflict (slug will be generated later)
        if (isBlank(request.slug())) {
            return false;
        }
        return postRepository.findByUsernameAndSlug(loggedUser.getUsername(), request.slug())
                             .filter(existingPost -> !existingPost.getId().equals(request.id()))
                             .isPresent();
    }

    private void updateCoverImage(Post post, SaveDraftRequest request) {
        if (request.coverId() != null && !request.coverId().isBlank()) {
            imageRepository.findByUuid(request.coverId()).ifPresent(post::setCover);
        } else if (post.getCover() != null) {
            // Explicitly remove cover if request has no coverId or empty coverId
            post.setCover(null);
        }
    }

    private Optional<Response> validateRequest(SaveDraftRequest request) {
        if (isBlank(request.content())) {
            return Optional.of(buildErrorResponse(ERROR_MSG_CONTENT_REQUIRED));
        }
        if (isBlank(request.title())) {
            return Optional.of(buildErrorResponse(ERROR_MSG_TITLE_REQUIRED));
        }
        if (hasInvalidSlugCharacters(request.slug())) {
            return Optional.of(buildErrorResponse(ERROR_MSG_INVALID_SLUG));
        }
        if (slugAlreadyExistsForDifferentPost(request)) {
            return Optional.of(buildErrorResponse(ERROR_MSG_SLUG_EXISTS));
        }
        return Optional.empty();
    }
}