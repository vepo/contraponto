package dev.vepo.contraponto.write;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublicationService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.tag.TagService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Logged
@Path("/write")
@ApplicationScoped
public class WriteEndpoint {
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance write(Optional<Post> post,
                                                    Links links,
                                                    LoggedUser user,
                                                    List<Blog> blogs,
                                                    Long selectedBlogId,
                                                    String initialTagsJson,
                                                    String initialSerieTitle,
                                                    boolean hasUnpublishedChanges);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final PostPublicationService publicationService;
    private final BlogRepository blogRepository;
    private final CustomPageRepository customPageRepository;
    private final TagService tagService;
    private final LoggedUser loggedUser;

    @Inject
    public WriteEndpoint(PostRepository postRepository,
                         PostPublicationService publicationService,
                         CustomPageRepository customPageRepository,
                         BlogRepository blogRepository,
                         TagService tagService,
                         LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.publicationService = publicationService;
        this.customPageRepository = customPageRepository;
        this.blogRepository = blogRepository;
        this.tagService = tagService;
        this.loggedUser = loggedUser;
    }

    private Long findDefaultBlogId(List<Blog> blogs) {
        return blogs.stream()
                    .filter(Blog::isMain)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No main blog! userId=%d".formatted(loggedUser.getId())))
                    .getId();
    }

    @GET
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance write() {
        var blogs = blogRepository.findActiveBlogs(loggedUser.getId());
        Long selectedBlogId = findDefaultBlogId(blogs);
        return Templates.write(Optional.empty(),
                               customPageRepository.loadLinks(),
                               loggedUser,
                               blogs,
                               selectedBlogId,
                               "[]",
                               "",
                               false);
    }

    @GET
    @Path("draft/{draftId}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance write(@PathParam("draftId") Long draftId) {
        var maybePost = Optional.ofNullable(draftId)
                                .flatMap(postRepository::findByIdWithTags)
                                .filter(post -> Objects.equals(post.getAuthor().getId(), loggedUser.getId()));
        if (maybePost.isEmpty()) {
            throw new NotFoundException("Draft not found! id=%s".formatted(draftId));
        }
        var blogs = blogRepository.findActiveBlogs(loggedUser.getId());
        Post post = maybePost.get();
        Long selectedBlogId = post.getBlog().getId(); // editing, preserve blog
        String initialTagsJson = tagService.tagsToJson(post);
        String initialSerieTitle = post.getSerie() != null ? post.getSerie().getTitle() : "";
        boolean hasUnpublishedChanges = publicationService.hasUnpublishedChanges(post);
        return Templates.write(maybePost, customPageRepository.loadLinks(), loggedUser,
                               blogs,
                               selectedBlogId,
                               initialTagsJson,
                               initialSerieTitle,
                               hasUnpublishedChanges);
    }
}