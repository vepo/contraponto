package dev.vepo.contraponto.git;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class BlogGitIntegrationTransaction {

    private final BlogGitIntegrationService integrationService;

    @Inject
    public BlogGitIntegrationTransaction(BlogGitIntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void exportPost(long postId) {
        try {
            integrationService.exportPostTransactional(postId);
        } catch (Exception e) {
            throw new IllegalStateException("Git push/export failed postId=%d".formatted(postId), e);
        }
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
