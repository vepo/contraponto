package dev.vepo.contraponto.git;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import io.quarkus.arc.Arc;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class BlogGitIntegrationTransaction {

    private static final Logger LOG = LoggerFactory.getLogger(BlogGitIntegrationTransaction.class);

    private static boolean isConfiguredForGit(Blog blog) {
        return blog.isGitEnabled()
                && blog.getGitRemoteUrl() != null
                && !blog.getGitRemoteUrl().isBlank();
    }

    private final BlogGitIntegrationService integrationService;
    private final BlogRepository blogRepository;
    private final PostRepository postRepository;

    private final GitSyncRunService gitSyncRunService;

    @Inject
    public BlogGitIntegrationTransaction(BlogGitIntegrationService integrationService,
                                         BlogRepository blogRepository,
                                         PostRepository postRepository,
                                         GitSyncRunService gitSyncRunService) {
        this.integrationService = integrationService;
        this.blogRepository = blogRepository;
        this.postRepository = postRepository;
        this.gitSyncRunService = gitSyncRunService;
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void exportPost(long postId) {
        try {
            integrationService.exportPostTransactional(postId);
        } catch (Exception e) {
            throw new IllegalStateException("Git push/export failed postId=%d".formatted(postId), e);
        }
    }

    @ActivateRequestContext
    public void runScheduledExport(long postId, GitSyncTrigger trigger) {
        try {
            Optional<Post> opt = postRepository.findById(postId);
            if (opt.isEmpty()) {
                return;
            }
            Blog blog = opt.get().getBlog();
            if (!(blog.isActive() && isConfiguredForGit(blog))) {
                return;
            }
            long runId = gitSyncRunService.beginRunForBlog(blog.getId(), GitSyncOperation.EXPORT, trigger, postId);
            GitSyncRunContext.setRunId(runId);
            self().exportPost(postId);
        } catch (RuntimeException e) {
            LOG.error("Git export failed postId={}", postId, e);
        } finally {
            GitSyncRunContext.clear();
        }
    }

    @ActivateRequestContext
    public void runScheduledImport(long blogId, GitSyncTrigger trigger) {
        try {
            Optional<Blog> blogOpt = blogRepository.findById(blogId);
            if (blogOpt.isEmpty() || !blogOpt.get().isActive() || !isConfiguredForGit(blogOpt.get())) {
                return;
            }
            long runId = gitSyncRunService.beginRunForBlog(blogId, GitSyncOperation.IMPORT, trigger, null);
            GitSyncRunContext.setRunId(runId);
            self().syncBlogFromGit(blogId);
        } catch (RuntimeException e) {
            LOG.error("Git import failed blogId={}", blogId, e);
        } finally {
            GitSyncRunContext.clear();
        }
    }

    private BlogGitIntegrationTransaction self() {
        return Arc.container().select(BlogGitIntegrationTransaction.class).get();
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void syncBlogFromGit(long blogId) {
        try {
            integrationService.syncBlogFromGitTransactional(blogId);
        } catch (Exception e) {
            throw new IllegalStateException("Git pull/import failed blogId=%d".formatted(blogId), e);
        }
    }
}
