package dev.vepo.contraponto.git;

import java.util.Objects;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_git_sync_run_entries")
public class GitSyncRunEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private GitSyncRun run;

    @Column(nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GitSyncPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private GitSyncEntryLevel level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "markdown_path", length = 1024)
    private String markdownPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GitSyncEntryOutcome outcome;

    @Column(nullable = false, length = 1024)
    private String message;

    @Column(length = 2048)
    private String remediation;

    @Column(name = "technical_detail", length = 4096)
    private String technicalDetail;

    public GitSyncRunEntry() {}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        GitSyncRunEntry other = (GitSyncRunEntry) obj;
        if (id == null || other.id == null) {
            return false;
        }
        return Objects.equals(other.id, id);
    }

    public Long getId() {
        return id;
    }

    public GitSyncEntryLevel getLevel() {
        return level;
    }

    public String getMarkdownPath() {
        return markdownPath;
    }

    public String getMessage() {
        return message;
    }

    public GitSyncEntryOutcome getOutcome() {
        return outcome;
    }

    public GitSyncPhase getPhase() {
        return phase;
    }

    public Post getPost() {
        return post;
    }

    public String getRemediation() {
        return remediation;
    }

    public GitSyncRun getRun() {
        return run;
    }

    public int getSequence() {
        return sequence;
    }

    public String getTechnicalDetail() {
        return technicalDetail;
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : Objects.hash(id);
    }

    public void setLevel(GitSyncEntryLevel level) {
        this.level = level;
    }

    public void setMarkdownPath(String markdownPath) {
        this.markdownPath = markdownPath;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setOutcome(GitSyncEntryOutcome outcome) {
        this.outcome = outcome;
    }

    public void setPhase(GitSyncPhase phase) {
        this.phase = phase;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void setRemediation(String remediation) {
        this.remediation = remediation;
    }

    public void setRun(GitSyncRun run) {
        this.run = run;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public void setTechnicalDetail(String technicalDetail) {
        this.technicalDetail = technicalDetail;
    }
}
