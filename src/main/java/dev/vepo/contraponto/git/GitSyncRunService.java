package dev.vepo.contraponto.git;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitSyncRunService {

    private final BlogRepository blogRepository;
    private final GitSyncRunRepository gitSyncRunRepository;
    private final GitSyncRunTransaction gitSyncRunTransaction;
    private final ObjectMapper objectMapper;

    @Inject
    public GitSyncRunService(BlogRepository blogRepository,
                             GitSyncRunRepository gitSyncRunRepository,
                             GitSyncRunTransaction gitSyncRunTransaction,
                             ObjectMapper objectMapper) {
        this.blogRepository = blogRepository;
        this.gitSyncRunRepository = gitSyncRunRepository;
        this.gitSyncRunTransaction = gitSyncRunTransaction;
        this.objectMapper = objectMapper;
    }

    public void appendEntry(long runId, GitSyncRunEntryDraft draft) {
        gitSyncRunTransaction.appendEntry(runId, draft);
    }

    public void appendEntryCurrent(GitSyncRunEntryDraft draft) {
        Long runId = GitSyncRunContext.currentRunId();
        if (runId != null) {
            appendEntry(runId, draft);
        }
    }

    public void appendPostResult(GitSyncPhase phase, GitSyncPostResult result) {
        GitSyncEntryLevel level = switch (result.outcome()) {
            case SUCCESS -> GitSyncEntryLevel.INFO;
            case SKIPPED -> GitSyncEntryLevel.WARN;
            case FAILED -> GitSyncEntryLevel.ERROR;
        };
        appendEntryCurrent(new GitSyncRunEntryDraft(
                                                    phase,
                                                    level,
                                                    result.postId(),
                                                    result.markdownPath(),
                                                    result.outcome(),
                                                    result.message(),
                                                    result.remediation(),
                                                    result.technicalDetail()));
    }

    public long beginRunForBlog(long blogId,
                                GitSyncOperation operation,
                                GitSyncTrigger trigger,
                                Long postId) {
        Blog blog = blogRepository.findById(blogId).orElseThrow();
        return gitSyncRunTransaction.beginRun(
                                              blogId,
                                              operation,
                                              trigger,
                                              postId,
                                              blog.getGitRemoteUrl(),
                                              blog.getGitBranch(),
                                              blog.getGitLastKnownCommit());
    }

    public String buildConventionSnapshot(JekyllLayoutConvention convention,
                                          String configSource,
                                          String parseWarning) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("posts_directory", convention.postsRelative());
        map.put("drafts_directory", convention.draftsRelative());
        map.put("assets_directory", convention.assetsRelative());
        map.put("layout_fm_key", convention.layoutFrontMatterKey());
        map.put("default_layout", convention.defaultLayoutValue());
        map.put("config_source", configSource);
        if (parseWarning != null && !parseWarning.isBlank()) {
            map.put("parse_warning", parseWarning);
        }
        return toJson(map);
    }

    public void finalizeRun(long runId, GitSyncRunResult result) {
        gitSyncRunTransaction.finalizeRun(runId, result);
    }

    public void finalizeRunCurrent(GitSyncRunResult result) {
        Long runId = GitSyncRunContext.currentRunId();
        if (runId != null) {
            finalizeRun(runId, result);
        }
    }

    public Page<GitSyncRun> listForBlog(long blogId, PageQuery query) {
        return gitSyncRunRepository.findPageByBlog(blogId, query);
    }

    public java.util.Optional<GitSyncRun> findForBlog(long blogId, long runId) {
        return gitSyncRunRepository.findByIdAndBlogId(runId, blogId);
    }

    public java.util.List<GitSyncRunEntry> listEntries(long runId) {
        return gitSyncRunRepository.listEntries(runId);
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
