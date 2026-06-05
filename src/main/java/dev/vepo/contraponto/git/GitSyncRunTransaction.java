package dev.vepo.contraponto.git;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.notification.NotificationService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

@ApplicationScoped
public class GitSyncRunTransaction {

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final GitSyncRunRepository gitSyncRunRepository;
    private final ContrapontoGitSettings gitSettings;
    private final NotificationService notificationService;

    private final ObjectMapper objectMapper;

    @Inject
    public GitSyncRunTransaction(BlogRepository blogRepository,
                                 PostRepository postRepository,
                                 GitSyncRunRepository gitSyncRunRepository,
                                 ContrapontoGitSettings gitSettings,
                                 NotificationService notificationService,
                                 ObjectMapper objectMapper) {
        this.blogRepository = blogRepository;
        this.postRepository = postRepository;
        this.gitSyncRunRepository = gitSyncRunRepository;
        this.gitSettings = gitSettings;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void appendEntry(long runId, GitSyncRunEntryDraft draft) {
        GitSyncRun run = gitSyncRunRepository.findById(runId).orElseThrow();
        GitSyncRunEntry entry = new GitSyncRunEntry();
        entry.setRun(run);
        entry.setSequence(gitSyncRunRepository.nextSequence(runId));
        entry.setPhase(draft.phase());
        entry.setLevel(draft.level());
        entry.setOutcome(draft.outcome());
        entry.setMessage(draft.message());
        entry.setRemediation(draft.remediation());
        entry.setTechnicalDetail(truncate(draft.technicalDetail(), 4096));
        entry.setMarkdownPath(draft.markdownPath());
        if (draft.postId() != null) {
            postRepository.findById(draft.postId()).ifPresent(entry::setPost);
        }
        gitSyncRunRepository.createEntry(entry);
    }

    private void applyNotificationPolicy(GitSyncRun run) {
        User owner = run.getBlog().getOwner();
        if (owner == null) {
            return;
        }
        boolean failed = run.getOutcome() == GitSyncOutcome.FAILED
                || run.getOutcome() == GitSyncOutcome.PARTIAL;
        boolean exportSuccess = run.getOperation() == GitSyncOperation.EXPORT
                && run.getOutcome() == GitSyncOutcome.SUCCESS;

        if (failed) {
            notificationService.notifyGitSyncFailed(owner, run.getBlog(), run);
        } else if (exportSuccess) {
            notificationService.notifyGitSyncSucceeded(owner, run.getBlog(), run);
        }
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public long beginRun(long blogId,
                         GitSyncOperation operation,
                         GitSyncTrigger trigger,
                         Long postId) {
        Blog blog = blogRepository.findById(blogId).orElseThrow();
        GitSyncRun run = new GitSyncRun();
        run.setBlog(blog);
        if (postId != null) {
            postRepository.findById(postId).ifPresent(run::setPost);
        }
        run.setOperation(operation);
        run.setTrigger(trigger);
        run.setOutcome(GitSyncOutcome.FAILED);
        run.setGitErrorKind(GitErrorKind.NONE);
        run.setRemoteUrl(blog.getGitRemoteUrl());
        run.setBranch(blog.getGitBranch());
        run.setCommitBefore(blog.getGitLastKnownCommit());
        run.setSettingsSnapshot(buildSettingsSnapshot());
        gitSyncRunRepository.create(run);
        return run.getId();
    }

    private String buildSettingsSnapshot() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("poll_enabled", gitSettings.pollEnabled());
        map.put("poll_interval", gitSettings.pollInterval());
        return toJson(map);
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void finalizeRun(long runId, GitSyncRunResult result) {
        GitSyncRun run = gitSyncRunRepository.findById(runId).orElseThrow();
        run.setOutcome(result.outcome());
        run.setGitErrorKind(result.gitErrorKind());
        run.setRepositoryReadable(result.repositoryReadable());
        run.setDataLoadable(result.dataLoadable());
        run.setCommitAfter(result.commitAfter());
        if (result.conventionSnapshot() != null) {
            run.setConventionSnapshot(result.conventionSnapshot());
        }
        if (result.settingsSnapshot() != null) {
            run.setSettingsSnapshot(result.settingsSnapshot());
        }
        run.setSummaryMessage(result.summaryMessage());
        run.setErrorDetail(truncate(result.errorDetail(), 4096));
        run.setFinishedAt(LocalDateTime.now(ZoneId.systemDefault()));
        gitSyncRunRepository.update(run);
        gitSyncRunRepository.pruneOldRuns(run.getBlog().getId());
        applyNotificationPolicy(run);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
