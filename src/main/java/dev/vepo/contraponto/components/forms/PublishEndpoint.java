package dev.vepo.contraponto.components.forms;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
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

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response publish(@BeanParam SaveDraftRequest request) {
        if (Objects.isNull(request.content()) || request.content().isBlank()) {
            return Response.status(Status.BAD_REQUEST)
                           .header("X-Toast-Message", "Content is required!")
                           .header("X-Toast-Type", "Error")
                           .header("X-Toast-Duration", "10000") // optional, in milliseconds
                           .build();
        }

        if (Objects.isNull(request.title()) || request.title().isBlank()) {
            return Response.status(Status.BAD_REQUEST)
                           .header("X-Toast-Message", "Title is required!")
                           .header("X-Toast-Type", "Error")
                           .header("X-Toast-Duration", "10000") // optional, in milliseconds
                           .build();
        }

        if (Objects.nonNull(request.slug()) && INVALID_SLUG_CHARS.matcher(request.slug()).find()) {
            return Response.status(Status.BAD_REQUEST)
                           .header("X-Toast-Message", "Slug can only contain lowercase letters, numbers, and hyphens")
                           .header("X-Toast-Type", "Error")
                           .header("X-Toast-Duration", "10000") // optional, in milliseconds
                           .build();
        }

        if (postRepository.findByUsernameAndSlug(loggedUser.getUsername(), request.slug())
                          .filter(p -> !Objects.equals(p.getId(), request.id()))
                          .isPresent()) {
            return Response.status(Status.BAD_REQUEST)
                           .header("X-Toast-Message", "Slug already exists!")
                           .header("X-Toast-Type", "Error")
                           .header("X-Toast-Duration", "10000") // optional, in milliseconds
                           .build();
        }

        Post post;
        if (Objects.nonNull(request.id())) {
            post = postRepository.findById(request.id()).orElseGet(Post::new);
        } else {
            post = new Post();
        }

        // Set cover image if provided
        if (request.coverId() != null && !request.coverId().isBlank()) {
            imageRepository.findByUuid(request.coverId())
                           .ifPresent(post::setCover);
        } else if (Objects.nonNull(post.getCover())) {
            post.setCover(null);
        }

        post.setSlug(request.slug());
        post.setAuthor(loggedUser.getUser());
        post.setTitle(request.title());
        post.setContent(request.content());
        if (Objects.isNull(request.slug()) || request.slug().isBlank()) {
            post.setSlug(request.title().toLowerCase().replaceAll("[^a-zA-Z0-9\\-]", "-"));
        } else {
            post.setSlug(request.slug());
        }
        post.setDescription(request.description());
        if (!post.isPublished()) {
            post.setPublished(true);
            post.setPublishedAt(LocalDateTime.now());
        }
        postRepository.save(post);
        return Response.ok()
                       .header("X-Toast-Message", "Post published!")
                       .header("X-Toast-Type", "Success")
                       .header("X-Toast-Duration", "10000") // optional, in milliseconds
                       .header("HX-Push-Url", "/%s/post/%s".formatted(post.getAuthor().getUsername(), post.getSlug()))
                       .entity(PostEndpoint.Templates.post(post, loggedUser, 0l))
                       .type(MediaType.TEXT_HTML)
                       .build();
    }
}
