package dev.vepo.contraponto.git.legacylink;

import dev.vepo.contraponto.git.BlogGitIntegrationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitLegacyLinkPatternChangedObserver {

    private final BlogGitIntegrationService blogGitIntegrationService;

    @Inject
    public GitLegacyLinkPatternChangedObserver(BlogGitIntegrationService blogGitIntegrationService) {
        this.blogGitIntegrationService = blogGitIntegrationService;
    }

    void afterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) BlogGitLegacyLinkPatternChangedEvent event) {
        blogGitIntegrationService.scheduleLegacyLinkReprocess(event.blogId());
    }
}
