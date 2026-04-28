package dev.vepo.contraponto.components.forms;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostRepository;
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

    private static final Pattern INVALID_SLUG_CHARS = Pattern.compile("[^a-z0-9\\-]");
    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public PublishEndpoint(PostRepository postRepository, ImageRepository imageRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.loggedUser = loggedUser;
    }

    private Response buildErrorResponse(String message) {
        return Toast.response(Status.BAD_REQUEST).message(message)
                    .type(Toast.Type.ERROR)
                    .duration(10_000)
                    .build();
    }

    private Response buildSuccessResponse(Post post) {
        return Toast.ok()
                    .message("Post published!")
                    .type(Toast.Type.SUCCESS)
                    .duration(10000)
                    .url("/%s/post/%s".formatted(post.getAuthor().getUsername(), post.getSlug()))
                    .page(PostEndpoint.Templates.post(post, loggedUser, 0L))
                    .build();
    }

    private void generateSlugIfNeeded(Post post, SaveDraftRequest request) {
        if (isBlank(request.slug())) {
            String generatedSlug = request.title().toLowerCase().replaceAll("[^a-zA-Z0-9\\-]", "-");
            post.setSlug(generatedSlug);
        } else {
            post.setSlug(request.slug());
        }
    }

    private Post getOrCreatePost(SaveDraftRequest request) {
        if (Objects.nonNull(request.id())) {
            return postRepository.findById(request.id()).orElseGet(Post::new);
        }
        return new Post();
    }

    private void handleCoverImage(Post post, SaveDraftRequest request) {
        if (request.coverId() != null && !request.coverId().isBlank()) {
            imageRepository.findByUuid(request.coverId()).ifPresent(post::setCover);
        } else if (Objects.nonNull(post.getCover())) {
            post.setCover(null);
        }
    }

    private boolean hasInvalidSlugChars(String slug) {
        return Objects.nonNull(slug) && INVALID_SLUG_CHARS.matcher(slug).find();
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
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
        handleCoverImage(post, request);
        setPostFields(post, request);
        generateSlugIfNeeded(post, request);
        publishIfNeeded(post);
        postRepository.save(post);

        return buildSuccessResponse(post);
    }

    private void publishIfNeeded(Post post) {
        if (!post.isPublished()) {
            post.setPublished(true);
            post.setPublishedAt(LocalDateTime.now());
        }
    }

    private void setPostFields(Post post, SaveDraftRequest request) {
        post.setAuthor(loggedUser.getUser());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setDescription(request.description());
    }

    private boolean slugAlreadyExists(SaveDraftRequest request) {
        return postRepository.findByUsernameAndSlug(loggedUser.getUsername(), request.slug())
                             .filter(p -> !Objects.equals(p.getId(), request.id()))
                             .isPresent();
    }

    private Optional<Response> validateRequest(SaveDraftRequest request) {
        if (isBlank(request.content())) {
            return Optional.of(buildErrorResponse("Content is required!"));
        }

        if (isBlank(request.title())) {
            return Optional.of(buildErrorResponse("Title is required!"));
        }

        if (hasInvalidSlugChars(request.slug())) {
            return Optional.of(buildErrorResponse("Slug can only contain lowercase letters, numbers, and hyphens"));
        }

        if (slugAlreadyExists(request)) {
            return Optional.of(buildErrorResponse("Slug already exists!"));
        }

        return Optional.empty();
    }
}