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
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.renderer.Format;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.TransactionRequiredException;

@ApplicationScoped
public class BlogGitIntegrationService {

    private record ConventionLoad(JekyllLayoutConvention convention, String configSource, String parseWarning) {}

    private static final Logger LOG = LoggerFactory.getLogger(BlogGitIntegrationService.class);

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

    private static boolean directoryHasOccupyingChildren(Path workspace) throws IOException {
        if (!Files.isDirectory(workspace)) {
            return false;
        }
        try (Stream<Path> listing = Files.list(workspace)) {
            return listing.findAny().isPresent();
        }
    }

    private static boolean isPostPath(String repoRelativePath, JekyllLayoutConvention convention) {
        String norm = repoRelativePath.replace('\\', '/');
        String posts = convention.postsRelative() + "/";
        String drafts = convention.draftsRelative() + "/";
        return norm.startsWith(posts) || norm.startsWith(drafts);
    }

    private static boolean isTransactionRequired(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TransactionRequiredException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static GitSyncPhase phaseForFailure(Exception e, GitErrorKind kind) {
        if (isTransactionRequired(e)) {
            return GitSyncPhase.FETCH;
        }
        return switch (kind) {
            case WORKSPACE -> GitSyncPhase.WORKSPACE;
            case CONVENTION -> GitSyncPhase.CONVENTION;
            case POST -> GitSyncPhase.POST_IMPORT;
            case NETWORK, REPOSITORY, AUTHENTICATION, UNKNOWN, NONE -> GitSyncPhase.FETCH;
        };
    }

    private static String posixPath(Path relative) {
        return relative.toString().replace('\\', '/');
    }

    private static String postExtension(Post post) {
        return post.getFormat() == Format.ASCIIDOC ? ".adoc" : ".md";
    }

    private static AbstractTreeIterator treeParser(Repository repo, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(objectId);
            CanonicalTreeParser parser = new CanonicalTreeParser();
            try (ObjectReader reader = repo.newObjectReader()) {
                parser.reset(reader, commit.getTree());
            }
            return parser;
        }
    }

    private final ContrapontoGitSettings gitSettings;
    private final BlogRepository blogRepository;

    private final PostRepository postRepository;

    private final BlogGitImportService blogGitImportService;

    private final PostGitMarkdownCodec markdownCodec;

    private final GitImageSyncService gitImageSyncService;

    private final BlogGitIntegrationTransaction integrationTransaction;

    private final GitSyncRunService gitSyncRunService;

    private final GitSyncErrorClassifier errorClassifier;

    @Inject
    public BlogGitIntegrationService(ContrapontoGitSettings gitSettings,
                                     BlogRepository blogRepository,
                                     PostRepository postRepository,
                                     BlogGitImportService blogGitImportService,
                                     PostGitMarkdownCodec markdownCodec,
                                     GitImageSyncService gitImageSyncService,
                                     BlogGitIntegrationTransaction integrationTransaction,
                                     GitSyncRunService gitSyncRunService,
                                     GitSyncErrorClassifier errorClassifier) {
        this.gitSettings = gitSettings;
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
        String user = gitSettings.username().orElse("").strip();
        String pass = gitSettings.password().orElse("").strip();
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

    private void fetchAndPull(Git git, CredentialsProvider credentials) throws GitAPIException {
        int depth = gitSettings.cloneDepth();
        var fetchCmd = git.fetch();
        if (credentials != null) {
            fetchCmd.setCredentialsProvider(credentials);
        }
        if (depth > 0) {
            fetchCmd.setDepth(depth);
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
                                                                        phaseForFailure(e, classified.kind()),
                                                                        classified.message(),
                                                                        classified.remediation(),
                                                                        e.toString()));
        finalizeRun(GitSyncOutcome.FAILED, classified.kind(), repositoryReadable, dataLoadable,
                    null, conventionSnapshot, classified.message());
    }

    private List<GitSyncPostResult> ingestPostFiles(long blogId,
                                                    Path workspace,
                                                    JekyllLayoutConvention convention,
                                                    List<Path> postFiles) {
        List<GitSyncPostResult> results = new ArrayList<>();
        Path postsRoot = convention.resolvePosts(workspace).toAbsolutePath().normalize();
        for (Path postFile : postFiles) {
            BlogGitImportService.SourceKind kind = postFile.startsWith(postsRoot)
                                                                                  ? BlogGitImportService.SourceKind.POSTS_FOLDER
                                                                                  : BlogGitImportService.SourceKind.DRAFTS_FOLDER;
            GitSyncPostResult result = blogGitImportService.ingest(blogId, workspace, convention, postFile, kind);
            gitSyncRunService.appendPostResult(GitSyncPhase.POST_IMPORT, result);
            results.add(result);
        }
        return results;
    }

    private boolean isConfiguredForGit(Blog blog) {
        return blog.isGitEnabled()
                && blog.getGitRemoteUrl() != null
                && !blog.getGitRemoteUrl().isBlank();
    }

    private boolean isImportablePostFile(Path p) {
        Path fname = p.getFileName();
        if (fname == null) {
            return false;
        }
        String n = fname.toString().toLowerCase(java.util.Locale.ROOT);
        return n.endsWith(".md")
                || n.endsWith(".markdown")
                || n.endsWith(".adoc")
                || n.endsWith(".asciidoc");
    }

    private List<Path> listAllPostFiles(Path workspace, JekyllLayoutConvention convention) throws IOException {
        List<Path> all = new ArrayList<>();
        all.addAll(listPostFilesInRoot(workspace, convention.resolvePosts(workspace)));
        all.addAll(listPostFilesInRoot(workspace, convention.resolveDrafts(workspace)));
        return all;
    }

    private List<Path> listChangedPostFiles(Path workspace,
                                            JekyllLayoutConvention convention,
                                            String lastKnown,
                                            String head)
            throws GitAPIException, IOException {
        if (lastKnown == null || lastKnown.isBlank()) {
            return listAllPostFiles(workspace, convention);
        }
        if (head == null || head.isBlank() || head.equals(lastKnown)) {
            return List.of();
        }
        try (Git git = Git.open(workspace.toFile())) {
            Repository repo = git.getRepository();
            ObjectId oldId = repo.resolve(lastKnown);
            ObjectId newId = repo.resolve(head);
            if (oldId == null) {
                LOG.info("Last known commit {} not in workspace; importing all posts.", lastKnown);
                return listAllPostFiles(workspace, convention);
            }
            if (newId == null || oldId.equals(newId)) {
                return List.of();
            }
            List<DiffEntry> diffs = git.diff()
                                       .setOldTree(treeParser(repo, oldId))
                                       .setNewTree(treeParser(repo, newId))
                                       .call();
            Path repoRoot = workspace.toAbsolutePath().normalize();
            List<Path> changed = new ArrayList<>();
            for (DiffEntry entry : diffs) {
                if (entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                    continue;
                }
                String rel = entry.getNewPath();
                if (!isPostPath(rel, convention)) {
                    continue;
                }
                Path file = repoRoot.resolve(rel);
                if (Files.isRegularFile(file) && isImportablePostFile(file)) {
                    changed.add(file);
                }
            }
            return changed;
        }
    }

    private List<Path> listPostFilesInRoot(Path workspace, Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return files;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> Files.isRegularFile(p) && isImportablePostFile(p)).forEach(files::add);
        }
        return files;
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
        String ext = postExtension(post);
        if (!post.isPublished()) {
            return convention.resolveDrafts(repoRoot).resolve(post.getSlug() + ext);
        }
        LocalDate pub =
                post.getPublishedAt() != null ? post.getPublishedAt().toLocalDate() : LocalDate.now(java.time.ZoneId.systemDefault());
        String name = "%s-%s%s".formatted(pub.format(DateTimeFormatter.ISO_LOCAL_DATE), post.getSlug(), ext);
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
            int depth = gitSettings.cloneDepth();
            if (depth > 0) {
                cmd.setDepth(depth);
            }
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

    private Optional<String> resolveRemoteHead(Blog blog) {
        try {
            LsRemoteCommand lsRemote = Git.lsRemoteRepository().setRemote(blog.getGitRemoteUrl()).setHeads(true);
            CredentialsProvider credentials = credentialsOrNull();
            if (credentials != null) {
                lsRemote.setCredentialsProvider(credentials);
            }
            Map<String, Ref> refs = lsRemote.callAsMap();
            String branch = resolveBranch(blog);
            Ref ref = refs.get(Constants.R_HEADS + branch);
            if (ref == null) {
                ref = refs.get("refs/heads/" + branch);
            }
            if (ref == null || ref.getObjectId() == null) {
                return Optional.empty();
            }
            return Optional.of(ref.getObjectId().name());
        } catch (GitAPIException e) {
            LOG.warn("Could not resolve remote HEAD for blogId={}: {}", blog.getId(), e.toString());
            return Optional.empty();
        }
    }

    public void scheduleBlogRemoteSync(long blogId) {
        scheduleBlogRemoteSync(blogId, GitSyncTrigger.REMOTE_POLL);
    }

    public void scheduleBlogRemoteSync(long blogId, GitSyncTrigger trigger) {
        CompletableFuture.runAsync(() -> integrationTransaction.runScheduledImport(blogId, trigger));
    }

    public void scheduleExportPost(long postId) {
        scheduleExportPost(postId, GitSyncTrigger.PUBLISH);
    }

    public void scheduleExportPost(long postId, GitSyncTrigger trigger) {
        CompletableFuture.runAsync(() -> integrationTransaction.runScheduledExport(postId, trigger));
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
            Optional<String> remoteHead = resolveRemoteHead(blog);
            String lastKnown = blog.getGitLastKnownCommit();
            if (remoteHead.isPresent() && remoteHead.get().equals(lastKnown)) {
                gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                               GitSyncPhase.FETCH,
                                                                               "Remote unchanged at "
                                                                                       + remoteHead.get().substring(0, Math.min(7, remoteHead.get().length()))
                                                                                       + "."));
                finalizeRun(GitSyncOutcome.SKIPPED, GitErrorKind.NONE, false, false,
                            remoteHead.get(), null, "No new commits on remote.");
                return;
            }

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

            String head = resolveHead(workspace);
            if (head != null && head.equals(lastKnown)) {
                gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                               GitSyncPhase.FETCH,
                                                                               "Workspace HEAD matches last known commit."));
                finalizeRun(GitSyncOutcome.SKIPPED, GitErrorKind.NONE, true, true,
                            head, null, "No new commits on remote.");
                return;
            }

            ConventionLoad loaded = loadConvention(workspace);
            repositoryReadable = true;
            conventionSnapshot = gitSyncRunService.buildConventionSnapshot(
                                                                           loaded.convention(), loaded.configSource(), loaded.parseWarning());
            JekyllLayoutConvention convention = loaded.convention();

            List<Path> changedPosts = listChangedPostFiles(workspace, convention, lastKnown, head);
            gitSyncRunService.appendEntryCurrent(GitSyncRunEntryDraft.info(
                                                                           GitSyncPhase.POST_IMPORT,
                                                                           changedPosts.size() + " post file(s) to import."));

            postResults.addAll(ingestPostFiles(blogId, workspace, convention, changedPosts));

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

    private Path workspaceForBlog(Long blogId) {
        return workspaceRoot().resolve("blog-%d".formatted(blogId));
    }

    private Path workspaceRoot() {
        Optional<String> root = gitSettings.workspaceRoot();
        if (root.isEmpty() || root.get().strip().isEmpty()) {
            return Path.of(System.getProperty("java.io.tmpdir")).resolve("contraponto-git");
        }
        return Path.of(root.get().strip());
    }
}
