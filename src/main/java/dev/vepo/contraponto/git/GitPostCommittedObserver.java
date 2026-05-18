package dev.vepo.contraponto.git;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitPostCommittedObserver {

    private final BlogGitIntegrationService blogGitIntegrationService;

    @Inject
    public GitPostCommittedObserver(BlogGitIntegrationService blogGitIntegrationService) {
        this.blogGitIntegrationService = blogGitIntegrationService;
    }

    void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) PostGitSyncRequestedEvent event) {
        blogGitIntegrationService.scheduleExportPost(event.postId(), event.trigger());
    }
}
