package dev.vepo.contraponto.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BlogGitIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(BlogGitIntegrationService.class);

    private record ConventionLoad(JekyllLayoutConvention convention, String configSource, String parseWarning) {}

    private static boolean directoryHasOccupyingChildren(Path workspace) throws IOException {
        if (!Files.isDirectory(workspace)) {
            return false;
        }
        try (Stream<Path> listing = Files.list(workspace)) {
            return listing.findAny().isPresent();
        }
    }

    private static String posixPath(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private final ContrapontoGitConfig config;
    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final BlogGitImportService blogGitImportService;
    private final PostGitMarkdownCodec markdownCodec;
    private final GitImageSyncService gitImageSyncService;
    private final BlogGitIntegrationTransaction integrationTransaction;
    private final GitSyncRunService gitSyncRunService;
    private final GitSyncErrorClassifier errorClassifier;

    @Inject
    public BlogGitIntegrationService(ContrapontoGitConfig config,
                                     BlogRepository blogRepository,
                                     PostRepository postRepository,
                                     BlogGitImportService blogGitImportService,
                                     PostGitMarkdownCodec markdownCodec,
                                     GitImageSyncService gitImageSyncService,
                                     BlogGitIntegrationTransaction integrationTransaction,
                                     GitSyncRunService gitSyncRunService,
                                     GitSyncErrorClassifier errorClassifier) {
        this.config = config;
        this.blogRepository = blogRepository;
        this.postRepository = postRepository;
        this.blogGitImportService = blogGitImportService;
        this.markdownCodec = markdownCodec;
        this.gitImageSyncService = gitImageSyncService;
        this.integrationTransaction = integrationTransaction;
        this.gitSyncRunService = gitSyncRunService;
        this.errorClassifier = errorClassifier;
    }

    private CredentialsProvider credentialsOrNull() {
        String user = config.username().orElse("").strip();
        String pass = config.password().orElse("").strip();
        if (user.isEmpty() && pass.isEmpty()) {
            return null;
        }
        return new UsernamePasswordCredentialsProvider(user, pass);
    }

    void exportPostTransactional(long postId) throws Exception {
        Optional<Post> opt = postRepository.findByIdWithTags(postId);
        if (opt.isEmpty()) {
            finalizeSkipped("Post was not found.");
            return;
        }
        Post post = opt.get();
        Blog blog = post.getBlog();
        if (!(blog.isActive() && isConfiguredForGit(blog))) {
            finalizeSkipped("Git integration is not configured for this blog.");
            return;
        }

        Path workspace = workspaceForBlog(blog.getId());
        boolean dataLoadable = false;
        boolean repositoryReadable = false;
        String conventionSnapshot = null;

        try {
            if (!prepareWorkspace(blog, workspace)) {
                gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.error(
                                                                                GitSyncPhase.WORKSPACE,
                                                                                "Git workspace could not be prepared.",
                                                                                GitSyncErrorClassifier.defaultRemediation(GitErrorKind.WORKSPACE),
                                                                                "Occupied non-git directory"));
                finalizeRun(GitSyncOutcome.SKIPPED, GitErrorKind.WORKSPACE, false, false,
                            null, conventionSnapshot, "Git sync skipped: workspace not available.");
                return;
            }
            dataLoadable = true;
            gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                           GitSyncPhase.WORKSPACE, "Git workspace prepared."));

            CredentialsProvider credentials = credentialsOrNull();
            try (Git git = Git.open(workspace.toFile())) {
                fetchAndPull(git, credentials);
                gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                               GitSyncPhase.PULL, "Remote changes pulled."));

                ConventionLoad loaded = loadConvention(workspace);
                repositoryReadable = true;
                conventionSnapshot = gitSyncRunService.buildConventionSnapshot(
                                                                               loaded.convention(), loaded.configSource(), loaded.parseWarning());
                gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                               GitSyncPhase.CONVENTION,
                                                                               "Repository layout resolved (" + loaded.configSource() + ")."));

                JekyllLayoutConvention convention = loaded.convention();
                Path markdownPath = markdownPathForPost(post, convention, workspace);
                Files.createDirectories(markdownPath.getParent());

                PostPublication live = post.getLivePublication();
                LinkedHashMap<String, Object> fm =
                        BlogGitMarkdownMapper.buildFrontMatter(post, convention);
                gitImageSyncService.addCoverFrontMatter(fm, post, convention);
                String rawBody;
                if (live != null && live.getContent() != null) {
                    rawBody = live.getContent();
                } else {
                    rawBody = post.getContent() == null ? "" : post.getContent();
                }
                String body = gitImageSyncService.prepareBodyForExport(rawBody, convention);
                String markdown = markdownCodec.writeMarkdownDocument(fm, body);
                Files.writeString(markdownPath, markdown, StandardCharsets.UTF_8);

                gitImageSyncService.exportImagesForPost(git, workspace, convention, post, body);

                Path repoRoot = workspace.toAbsolutePath().normalize();
                Path targetAbs = markdownPath.toAbsolutePath().normalize();
                String rel = posixPath(repoRoot.relativize(targetAbs));
                git.add().addFilepattern(rel).call();

                gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                               GitSyncPhase.POST_EXPORT,
                                                                               "Exported post \"" + post.getSlug() + "\" to " + rel + "."));

                var status = git.status().call();
                if (status.hasUncommittedChanges()) {
                    PersonIdent who = new PersonIdent("Contraponto", "noreply@contraponto.local");
                    git.commit()
                       .setAllowEmpty(false)
                       .setAuthor(who)
                       .setCommitter(who)
                       .setMessage("[contraponto] sync post " + post.getSlug())
                       .call();
                    gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                                   GitSyncPhase.COMMIT, "Changes committed locally."));

                    var push = git.push();
                    if (credentials != null) {
                        push.setCredentialsProvider(credentials);
                    }
                    push.call();
                    gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                                   GitSyncPhase.PUSH, "Changes pushed to remote."));
                } else {
                    gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                                   GitSyncPhase.PUSH, "No changes to push."));
                }

                Blog managed = blogRepository.findById(blog.getId()).orElseThrow();
                ObjectId headId = git.getRepository().resolve(Constants.HEAD);
                String commitAfter = headId != null ? headId.name() : null;
                if (commitAfter != null) {
                    managed.setGitLastKnownCommit(commitAfter);
                    blogRepository.save(managed);
                }
                finalizeRun(GitSyncOutcome.SUCCESS, GitErrorKind.NONE, repositoryReadable, dataLoadable,
                            commitAfter, conventionSnapshot,
                            "Git export succeeded for post \"" + post.getSlug() + "\".");
            }
        } catch (Exception e) {
            handleFailure(e, repositoryReadable, dataLoadable, conventionSnapshot);
            throw e;
        }
    }

    private void exportPostWithNewTransaction(long postId, GitSyncTrigger trigger) {
        long runId = 0;
        try {
            Optional<Post> opt = postRepository.findById(postId);
            if (opt.isEmpty()) {
                return;
            }
            Blog blog = opt.get().getBlog();
            if (!(blog.isActive() && isConfiguredForGit(blog))) {
                return;
            }
            runId = gitSyncRunService.beginRunForBlog(blog.getId(), GitSyncOperation.EXPORT, trigger, postId);
            GitSyncRunContext.setRunId(runId);
            integrationTransaction.exportPost(postId);
        } catch (RuntimeException e) {
            LOG.error("Git export failed postId={}", postId, e);
        } finally {
            GitSyncRunContext.clear();
        }
    }

    private void fetchAndPull(Git git, CredentialsProvider credentials) throws GitAPIException {
        var fetchCmd = git.fetch();
        if (credentials != null) {
            fetchCmd.setCredentialsProvider(credentials);
        }
        fetchCmd.call();
        gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(GitSyncPhase.FETCH, "Fetched from remote."));

        var pullCmd = git.pull();
        if (credentials != null) {
            pullCmd.setCredentialsProvider(credentials);
        }
        pullCmd.call();
    }

    private void finalizeRun(GitSyncOutcome outcome,
                             GitErrorKind errorKind,
                             boolean repositoryReadable,
                             boolean dataLoadable,
                             String commitAfter,
                             String conventionSnapshot,
                             String summary) {
        gitSyncRunService.finalizeRunCurrent(new GitSyncRunResult(
                                                                  outcome,
                                                                  errorKind,
                                                                  repositoryReadable,
                                                                  dataLoadable,
                                                                  commitAfter,
                                                                  conventionSnapshot,
                                                                  null,
                                                                  summary,
                                                                  null));
    }

    private void finalizeSkipped(String summary) {
        finalizeRun(GitSyncOutcome.SKIPPED, GitErrorKind.NONE, false, false, null, null, summary);
    }

    private void handleFailure(Exception e,
                               boolean repositoryReadable,
                               boolean dataLoadable,
                               String conventionSnapshot) {
        GitSyncErrorClassifier.ClassifiedError classified = errorClassifier.classify(e);
        gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.error(
                                                                        GitSyncPhase.WORKSPACE,
                                                                        classified.message(),
                                                                        classified.remediation(),
                                                                        e.toString()));
        finalizeRun(GitSyncOutcome.FAILED, classified.kind(), repositoryReadable, dataLoadable,
                    null, conventionSnapshot, classified.message());
    }

    private boolean isConfiguredForGit(Blog blog) {
        return blog.isGitEnabled()
                && blog.getGitRemoteUrl() != null
                && !blog.getGitRemoteUrl().isBlank();
    }

    private boolean isMarkdown(Path p) {
        Path fname = p.getFileName();
        if (fname == null) {
            return false;
        }
        String n = fname.toString().toLowerCase(java.util.Locale.ROOT);
        return n.endsWith(".md") || n.endsWith(".markdown");
    }

    private ConventionLoad loadConvention(Path workspace) {
        Path cfg = workspace.resolve(JekyllLayoutConvention.CONFIG_FILENAME);
        try {
            if (Files.isReadable(cfg)) {
                Map<String, Object> map = markdownCodec.readYamlObjectMap(cfg);
                return new ConventionLoad(
                                          JekyllLayoutConvention.fromYaml(Optional.ofNullable(map).orElseGet(LinkedHashMap::new)),
                                          "_contraponto.yml",
                                          null);
            }
        } catch (IOException | IllegalArgumentException e) {
            LOG.warn("Could not parse {}, using defaults: {}", cfg.toAbsolutePath(), e.toString());
            return new ConventionLoad(JekyllLayoutConvention.defaults(), "defaults", e.toString());
        }
        return new ConventionLoad(JekyllLayoutConvention.defaults(), "defaults", null);
    }

    private Path markdownPathForPost(Post post, JekyllLayoutConvention convention, Path repoRoot) {
        if (!post.isPublished()) {
            return convention.resolveDrafts(repoRoot).resolve(post.getSlug() + ".md");
        }
        LocalDate pub =
                post.getPublishedAt() != null ? post.getPublishedAt().toLocalDate() : LocalDate.now(java.time.ZoneId.systemDefault());
        String name = "%s-%s.md".formatted(pub.format(DateTimeFormatter.ISO_LOCAL_DATE), post.getSlug());
        return convention.resolvePosts(repoRoot).resolve(name);
    }

    private boolean prepareWorkspace(Blog blog, Path workspace) throws GitAPIException, IOException {
        Files.createDirectories(workspace);
        CredentialsProvider credentials = credentialsOrNull();

        Path gitDot = workspace.resolve(".git");
        if (!Files.isDirectory(gitDot)) {
            if (directoryHasOccupyingChildren(workspace)) {
                LOG.warn("Refusing clone into occupied directory {}", workspace);
                return false;
            }

            CloneCommand cmd = Git.cloneRepository()
                                  .setURI(blog.getGitRemoteUrl())
                                  .setDirectory(workspace.toFile())
                                  .setBranch(resolveBranch(blog));
            if (credentials != null) {
                cmd.setCredentialsProvider(credentials);
            }
            try (Git g = cmd.call()) {
                g.getRepository().close();
            }
            return true;
        }

        try (Git git = Git.open(workspace.toFile())) {
            fetchAndPull(git, credentials);
            return true;
        }
    }

    private String resolveBranch(Blog blog) {
        String b = Objects.requireNonNullElse(blog.getGitBranch(), "").strip();
        return b.isEmpty() ? "main" : b;
    }

    private String resolveHead(Path workspace) {
        try (Git git = Git.open(workspace.toFile())) {
            ObjectId oid = git.getRepository().resolve(Constants.HEAD);
            return oid != null ? oid.name() : null;
        } catch (IOException ex) {
            LOG.warn("Could not resolve HEAD in {}", workspace, ex);
            return null;
        }
    }

    public void scheduleBlogRemoteSync(long blogId, GitSyncTrigger trigger) {
        CompletableFuture.runAsync(() -> syncBlogFromGit(blogId, trigger));
    }

    public void scheduleBlogRemoteSync(long blogId) {
        scheduleBlogRemoteSync(blogId, GitSyncTrigger.REMOTE_POLL);
    }

    public void scheduleExportPost(long postId, GitSyncTrigger trigger) {
        CompletableFuture.runAsync(() -> exportPostWithNewTransaction(postId, trigger));
    }

    public void scheduleExportPost(long postId) {
        scheduleExportPost(postId, GitSyncTrigger.PUBLISH);
    }

    private void syncBlogFromGit(long blogId, GitSyncTrigger trigger) {
        long runId = 0;
        try {
            Optional<Blog> blogOpt = blogRepository.findById(blogId);
            if (blogOpt.isEmpty() || !blogOpt.get().isActive() || !isConfiguredForGit(blogOpt.get())) {
                return;
            }
            runId = gitSyncRunService.beginRunForBlog(blogId, GitSyncOperation.IMPORT, trigger, null);
            GitSyncRunContext.setRunId(runId);
            integrationTransaction.syncBlogFromGit(blogId);
        } catch (RuntimeException e) {
            LOG.error("Git import failed blogId={}", blogId, e);
        } finally {
            GitSyncRunContext.clear();
        }
    }

    void syncBlogFromGitTransactional(long blogId) throws IOException, GitAPIException {
        Optional<Blog> blogOpt = blogRepository.findById(blogId);
        if (blogOpt.isEmpty()) {
            finalizeSkipped("Blog was not found.");
            return;
        }
        Blog blog = blogOpt.get();
        if (!blog.isActive() || !isConfiguredForGit(blog)) {
            finalizeSkipped("Git integration is not configured for this blog.");
            return;
        }

        boolean dataLoadable = false;
        boolean repositoryReadable = false;
        String conventionSnapshot = null;
        List<GitSyncPostResult> postResults = new ArrayList<>();

        try {
            Path workspace = workspaceForBlog(blogId);
            if (!prepareWorkspace(blog, workspace)) {
                gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.error(
                                                                                GitSyncPhase.WORKSPACE,
                                                                                "Git workspace could not be prepared.",
                                                                                GitSyncErrorClassifier.defaultRemediation(GitErrorKind.WORKSPACE),
                                                                                null));
                finalizeRun(GitSyncOutcome.SKIPPED, GitErrorKind.WORKSPACE, false, false,
                            null, null, "Git sync skipped: workspace not available.");
                return;
            }
            dataLoadable = true;

            ConventionLoad loaded = loadConvention(workspace);
            repositoryReadable = true;
            conventionSnapshot = gitSyncRunService.buildConventionSnapshot(
                                                                           loaded.convention(), loaded.configSource(), loaded.parseWarning());
            JekyllLayoutConvention convention = loaded.convention();

            gitImageSyncService.importAssetsFromWorkspace(blog, workspace, convention);
            gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                           GitSyncPhase.ASSETS, "Assets imported from repository."));

            postResults.addAll(walkImport(blogId, workspace, convention.resolvePosts(workspace),
                                          BlogGitImportService.SourceKind.POSTS_FOLDER));
            postResults.addAll(walkImport(blogId, workspace, convention.resolveDrafts(workspace),
                                          BlogGitImportService.SourceKind.DRAFTS_FOLDER));

            String head = resolveHead(workspace);
            blog.setGitLastKnownCommit(head);
            blogRepository.save(blog);

            GitSyncOutcome outcome = aggregatePostOutcomes(postResults);
            GitErrorKind errorKind = outcome == GitSyncOutcome.FAILED || outcome == GitSyncOutcome.PARTIAL
                                                                                                           ? GitErrorKind.POST
                                                                                                           : GitErrorKind.NONE;
            String summary = buildImportSummary(outcome, postResults);
            finalizeRun(outcome, errorKind, repositoryReadable, dataLoadable, head, conventionSnapshot, summary);
        } catch (Exception e) {
            handleFailure(e, repositoryReadable, dataLoadable, conventionSnapshot);
            throw e;
        }
    }

    private static GitSyncOutcome aggregatePostOutcomes(List<GitSyncPostResult> results) {
        if (results.isEmpty()) {
            return GitSyncOutcome.SUCCESS;
        }
        long failed = results.stream().filter(GitSyncPostResult::isFailed).count();
        long ok = results.stream().filter(GitSyncPostResult::isSuccess).count();
        if (failed == 0) {
            return GitSyncOutcome.SUCCESS;
        }
        if (ok == 0) {
            return GitSyncOutcome.FAILED;
        }
        return GitSyncOutcome.PARTIAL;
    }

    private static String buildImportSummary(GitSyncOutcome outcome, List<GitSyncPostResult> results) {
        long failed = results.stream().filter(GitSyncPostResult::isFailed).count();
        long ok = results.stream().filter(GitSyncPostResult::isSuccess).count();
        return switch (outcome) {
            case SUCCESS -> "Git import succeeded. " + ok + " post(s) synced.";
            case PARTIAL -> "Git import partially completed. " + ok + " succeeded, " + failed + " failed.";
            case FAILED -> "Git import failed. " + failed + " post(s) could not be imported.";
            case SKIPPED -> "Git import skipped.";
        };
    }

    private List<GitSyncPostResult> walkImport(long blogId,
                                               Path workspace,
                                               Path root,
                                               BlogGitImportService.SourceKind kind)
            throws IOException {
        List<GitSyncPostResult> results = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return results;
        }
        JekyllLayoutConvention convention = loadConvention(workspace).convention();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> Files.isRegularFile(p) && isMarkdown(p)).forEach(markdown -> {
                GitSyncPostResult result = blogGitImportService.ingest(blogId, workspace, convention, markdown, kind);
                gitSyncRunService.appendPostResult(GitSyncPhase.POST_IMPORT, result);
                results.add(result);
            });
        }
        return results;
    }

    private Path workspaceForBlog(Long blogId) {
        return workspaceRoot().resolve("blog-%d".formatted(blogId));
    }

    private Path workspaceRoot() {
        Optional<String> root = config.workspaceRoot();
        if (root.isEmpty() || root.get().strip().isEmpty()) {
            return Path.of(System.getProperty("java.io.tmpdir")).resolve("contraponto-git");
        }
        return Path.of(root.get().strip());
    }
}
