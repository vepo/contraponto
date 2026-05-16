package dev.vepo.contraponto.git;

import dev.vepo.contraponto.blog.BlogRepository;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitRemotePollScheduler {

    private final ContrapontoGitConfig config;
    private final BlogRepository blogRepository;
    private final BlogGitIntegrationService blogGitIntegrationService;

    @Inject
    public GitRemotePollScheduler(ContrapontoGitConfig config,
                                  BlogRepository blogRepository,
                                  BlogGitIntegrationService blogGitIntegrationService) {
        this.config = config;
        this.blogRepository = blogRepository;
        this.blogGitIntegrationService = blogGitIntegrationService;
    }

    @Scheduled(every = "${contraponto.git.poll-interval}", concurrentExecution = ConcurrentExecution.SKIP)
    void synchronizeEnabledBlogsFromGit() {
        if (!config.pollEnabled()) {
            return;
        }
        for (Long id : blogRepository.findActiveBlogIdsForGitPoll()) {
            blogGitIntegrationService.scheduleBlogRemoteSync(id);
        }
    }
}
