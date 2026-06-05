package dev.vepo.contraponto.comment;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;
import dev.vepo.contraponto.post.PostRepository;
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

@ApplicationScoped
@Path("{username}")
public class CommentComponentEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance comment(CommentView comment,
                                                      Post post,
                                                      String commentsUrl,
                                                      boolean authenticated,
                                                      boolean postOwner);

        public static native TemplateInstance commentForm(long postId, Long parentId, String commentsUrl);

        public static native TemplateInstance comments(CommentsSectionView section);

        public static native TemplateInstance replies(CommentView root,
                                                      Post post,
                                                      String commentsUrl,
                                                      java.util.List<CommentView> replies,
                                                      boolean authenticated,
                                                      boolean postOwner);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostRepository postRepository;
    private final PostCommentService commentService;
    private final LoggedUser loggedUser;

    @Inject
    public CommentComponentEndpoint(PostRepository postRepository,
                                    PostCommentService commentService,
                                    LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.commentService = commentService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("{blogSlug}/post/{slug}/components/comments")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogComments(@PathParam("username") String username,
                                         @PathParam("blogSlug") String blogSlug,
                                         @PathParam("slug") String slug) {
        Post post = postRepository.findBlogPost(username, blogSlug, slug).orElseThrow(NotFoundException::new);
        return renderComments(post);
    }

    @GET
    @Path("{blogSlug}/post/{slug}/components/comments/{commentId}/replies")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogReplies(@PathParam("username") String username,
                                        @PathParam("blogSlug") String blogSlug,
                                        @PathParam("slug") String slug,
                                        @PathParam("commentId") long commentId) {
        Post post = postRepository.findBlogPost(username, blogSlug, slug).orElseThrow(NotFoundException::new);
        return renderReplies(post, commentId);
    }

    private CommentViewerContext buildViewer(Post post) {
        if (!loggedUser.isAuthenticated()) {
            return CommentViewerContext.anonymous();
        }
        boolean isOwner = post.getAuthor().getId().equals(loggedUser.getId());
        return CommentViewerContext.of(loggedUser.getId(), isOwner);
    }

    @GET
    @Path("post/{slug}/components/comments")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mainBlogComments(@PathParam("username") String username,
                                             @PathParam("slug") String slug) {
        Post post = postRepository.findMainBlogPost(username, slug).orElseThrow(NotFoundException::new);
        return renderComments(post);
    }

    @GET
    @Path("post/{slug}/components/comments/{commentId}/replies")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mainBlogReplies(@PathParam("username") String username,
                                            @PathParam("slug") String slug,
                                            @PathParam("commentId") long commentId) {
        Post post = postRepository.findMainBlogPost(username, slug).orElseThrow(NotFoundException::new);
        return renderReplies(post, commentId);
    }

    public TemplateInstance renderComments(Post post) {
        CommentViewerContext viewer = buildViewer(post);
        String commentsUrl = "%s/components/comments".formatted(PostPaths.extractUrl(post));
        CommentsSectionView section = new CommentsSectionView(post,
                                                              commentsUrl,
                                                              commentService.buildRootViews(post.getId(), viewer),
                                                              commentService.findPendingViews(post.getId(), viewer),
                                                              loggedUser.isAuthenticated(),
                                                              viewer.postOwner(),
                                                              loggedUser);
        return Templates.comments(section);
    }

    public TemplateInstance renderReplies(Post post, long rootCommentId) {
        CommentViewerContext viewer = buildViewer(post);
        String commentsUrl = "%s/components/comments".formatted(PostPaths.extractUrl(post));

        CommentView rootView = commentService.buildRootViews(post.getId(), viewer).stream()
                                             .filter(c -> c.id() == rootCommentId)
                                             .findFirst()
                                             .orElseThrow(NotFoundException::new);

        var replies = commentService.buildReplyViews(rootCommentId, viewer);
        return Templates.replies(rootView,
                                 post,
                                 commentsUrl,
                                 replies,
                                 loggedUser.isAuthenticated(),
                                 viewer.postOwner());
    }
}
