package dev.vepo.contraponto.git;

import java.time.LocalDateTime;
import java.util.Objects;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_git_sync_runs")
public class GitSyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", nullable = false)
    private Blog blog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GitSyncOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_kind", nullable = false, length = 32)
    private GitSyncTrigger trigger;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GitSyncOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "git_error_kind", nullable = false, length = 32)
    private GitErrorKind gitErrorKind = GitErrorKind.NONE;

    @Column(name = "repository_readable", nullable = false)
    private boolean repositoryReadable;

    @Column(name = "data_loadable", nullable = false)
    private boolean dataLoadable;

    @Column(name = "remote_url", length = 2048)
    private String remoteUrl;

    @Column(length = 255)
    private String branch;

    @Column(name = "commit_before", length = 64)
    private String commitBefore;

    @Column(name = "commit_after", length = 64)
    private String commitAfter;

    @Column(name = "convention_snapshot", columnDefinition = "TEXT")
    private String conventionSnapshot;

    @Column(name = "settings_snapshot", columnDefinition = "TEXT")
    private String settingsSnapshot;

    @Column(name = "summary_message", length = 512)
    private String summaryMessage;

    @Column(name = "error_detail", length = 4096)
    private String errorDetail;

    public GitSyncRun() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GitSyncRun other = (GitSyncRun) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public Blog getBlog() {
        return blog;
    }

    public String getBranch() {
        return branch;
    }

    public String getCommitAfter() {
        return commitAfter;
    }

    public String getCommitBefore() {
        return commitBefore;
    }

    public String getConventionSnapshot() {
        return conventionSnapshot;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public GitErrorKind getGitErrorKind() {
        return gitErrorKind;
    }

    public Long getId() {
        return id;
    }

    public GitSyncOperation getOperation() {
        return operation;
    }

    public GitSyncOutcome getOutcome() {
        return outcome;
    }

    public Post getPost() {
        return post;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getSettingsSnapshot() {
        return settingsSnapshot;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }

    public GitSyncTrigger getTrigger() {
        return trigger;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public boolean isDataLoadable() {
        return dataLoadable;
    }

    public boolean isRepositoryReadable() {
        return repositoryReadable;
    }

    @PrePersist
    void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (outcome == null) {
            outcome = GitSyncOutcome.FAILED;
        }
        if (gitErrorKind == null) {
            gitErrorKind = GitErrorKind.NONE;
        }
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setCommitAfter(String commitAfter) {
        this.commitAfter = commitAfter;
    }

    public void setCommitBefore(String commitBefore) {
        this.commitBefore = commitBefore;
    }

    public void setConventionSnapshot(String conventionSnapshot) {
        this.conventionSnapshot = conventionSnapshot;
    }

    public void setDataLoadable(boolean dataLoadable) {
        this.dataLoadable = dataLoadable;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public void setGitErrorKind(GitErrorKind gitErrorKind) {
        this.gitErrorKind = gitErrorKind;
    }

    public void setOperation(GitSyncOperation operation) {
        this.operation = operation;
    }

    public void setOutcome(GitSyncOutcome outcome) {
        this.outcome = outcome;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public void setRepositoryReadable(boolean repositoryReadable) {
        this.repositoryReadable = repositoryReadable;
    }

    public void setSettingsSnapshot(String settingsSnapshot) {
        this.settingsSnapshot = settingsSnapshot;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public void setSummaryMessage(String summaryMessage) {
        this.summaryMessage = summaryMessage;
    }

    public void setTrigger(GitSyncTrigger trigger) {
        this.trigger = trigger;
    }
}
