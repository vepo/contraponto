package dev.vepo.contraponto.comment;

import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Logged
@ApplicationScoped
public class CommentManageEndpoint {

    @CheckedTemplate
    public static class Templates {
        static native TemplateInstance panel(Page<CommentManageRow> comments, String basePath);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final PostCommentRepository commentRepository;
    private final LoggedUser loggedUser;

    @Inject
    public CommentManageEndpoint(PostCommentRepository commentRepository, LoggedUser loggedUser) {
        this.commentRepository = commentRepository;
        this.loggedUser = loggedUser;
    }

    public TemplateInstance renderHubPanel(int page, String basePath) {
        var comments = commentRepository.findPendingPageForPostAuthor(loggedUser.getId(), PageQuery.forGrid(20, page))
                                        .map(CommentManageRow::from);
        return Templates.panel(comments, basePath);
    }
}
