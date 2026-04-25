package dev.vepo.contraponto.components.forms;

import java.util.Objects;

import dev.vepo.contraponto.image.ImageRepository;
import dev.vepo.contraponto.post.Post;
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

@Logged
@ApplicationScoped
@Path("/forms/write/draft")
public class SaveDraftEndpoint {

    private final PostRepository postRepository;
    private final ImageRepository imageRepository;
    private final LoggedUser loggedUser;

    @Inject
    public SaveDraftEndpoint(PostRepository postRepository, ImageRepository imageRepository, LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.imageRepository = imageRepository;
        this.loggedUser = loggedUser;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(@BeanParam SaveDraftRequest request) {
        if (Objects.isNull(request.content()) || request.content().isBlank()) {
            return Response.ok()
                           .header("X-Toast-Message", "Content is required!")
                           .header("X-Toast-Type", "Error")
                           .header("X-Toast-Duration", "10000") // optional, in milliseconds
                           .build();
        }

        if (Objects.isNull(request.title()) || request.title().isBlank()) {
            return Response.ok()
                           .header("X-Toast-Message", "Title is required!")
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
        }

        post.setSlug(request.slug());
        post.setAuthor(loggedUser.getUser());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setDescription(request.description());
        postRepository.save(post);
        return Response.ok()
                       .header("X-Toast-Message", "Draft saved successfully!")
                       .header("X-Toast-Type", "Success")
                       .header("X-Toast-Duration", "10000") // optional, in milliseconds
                       .header("HX-Push-Url", "/write/draft/%d".formatted(post.getId()))
                       .build();
    }
}
