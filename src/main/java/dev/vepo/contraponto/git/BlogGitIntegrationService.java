package dev.vepo.contraponto.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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
import dev.vepo.contraponto.post.PostRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BlogGitIntegrationService {

    private static final Logger LOG = LoggerFactory.getLogger(BlogGitIntegrationService.class);

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

    @Inject
    public BlogGitIntegrationService(ContrapontoGitConfig config,
                                     BlogRepository blogRepository,
                                     PostRepository postRepository,
                                     BlogGitImportService blogGitImportService,
                                     PostGitMarkdownCodec markdownCodec) {
        this.config = config;
        this.blogRepository = blogRepository;
        this.postRepository = postRepository;
        this.blogGitImportService = blogGitImportService;
        this.markdownCodec = markdownCodec;
    }

    private CredentialsProvider credentialsOrNull() {
        String user = config.username().orElse("").strip();
        String pass = config.password().orElse("").strip();
        if (user.isEmpty() && pass.isEmpty()) {
            return null;
        }
        return new UsernamePasswordCredentialsProvider(user, pass);
    }

    private void exportPostTransactional(long postId) throws Exception {
        Optional<Post> opt = postRepository.findByIdWithTags(postId);
        if (opt.isEmpty()) {
            return;
        }
        Post post = opt.get();
        Blog blog = post.getBlog();
        if (!(blog.isActive() && isConfiguredForGit(blog))) {
            return;
        }

        Path workspace = workspaceForBlog(blog.getId());
        if (!prepareWorkspace(blog, workspace)) {
            return;
        }

        CredentialsProvider credentials = credentialsOrNull();
        try (Git git = Git.open(workspace.toFile())) {
            var fetchCmd = git.fetch();
            if (credentials != null) {
                fetchCmd.setCredentialsProvider(credentials);
            }
            fetchCmd.call();

            var pullCmd = git.pull();
            if (credentials != null) {
                pullCmd.setCredentialsProvider(credentials);
            }
            pullCmd.call();

            JekyllLayoutConvention convention = loadConvention(workspace);
            Path markdownPath = markdownPathForPost(post, convention, workspace);

            Files.createDirectories(markdownPath.getParent());

            LinkedHashMap<String, Object> fm =
                    BlogGitMarkdownMapper.buildFrontMatter(post, convention);
            String markdown = markdownCodec.writeMarkdownDocument(fm, post.getContent() == null ? "" : post.getContent());
            Files.writeString(markdownPath, markdown, StandardCharsets.UTF_8);

            Path repoRoot = workspace.toAbsolutePath().normalize();
            Path targetAbs = markdownPath.toAbsolutePath().normalize();
            String rel = posixPath(repoRoot.relativize(targetAbs));
            git.add().addFilepattern(rel).call();

            var status = git.status().call();
            if (status.hasUncommittedChanges()) {
                PersonIdent who = new PersonIdent("Contraponto", "noreply@contraponto.local");
                git.commit()
                   .setAllowEmpty(false)
                   .setAuthor(who)
                   .setCommitter(who)
                   .setMessage("[contraponto] sync post " + post.getSlug())
                   .call();

                var push = git.push();
                if (credentials != null) {
                    push.setCredentialsProvider(credentials);
                }
                push.call();
            }

            Blog managed = blogRepository.findById(blog.getId()).orElseThrow();
            ObjectId headId = git.getRepository().resolve(Constants.HEAD);
            if (headId != null) {
                managed.setGitLastKnownCommit(headId.name());
                blogRepository.save(managed);
            }
        }
    }

    private void exportPostWithNewTransaction(long postId) {
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                try {
                    exportPostTransactional(postId);
                } catch (Exception e) {
                    throw new IllegalStateException("Git push/export failed postId=%d".formatted(postId), e);
                }
            });
        } catch (RuntimeException e) {
            LOG.error("Git export failed postId={}", postId, e);
        }
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

    private JekyllLayoutConvention loadConvention(Path workspace) {
        Path cfg = workspace.resolve(JekyllLayoutConvention.CONFIG_FILENAME);
        try {
            if (Files.isReadable(cfg)) {
                LinkedHashMap<String, Object> map = markdownCodec.readYamlObjectMap(cfg);
                return JekyllLayoutConvention.fromYaml(Optional.ofNullable(map).orElseGet(LinkedHashMap::new));
            }
        } catch (IOException e) {
            LOG.warn("Could not parse {}, using defaults: {}", cfg.toAbsolutePath(), e.toString());
        }
        return JekyllLayoutConvention.defaults();
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
            var fetchCmd = git.fetch();
            if (credentials != null) {
                fetchCmd.setCredentialsProvider(credentials);
            }
            fetchCmd.call();

            var pullCmd = git.pull();
            if (credentials != null) {
                pullCmd.setCredentialsProvider(credentials);
            }
            pullCmd.call();
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

    public void scheduleBlogRemoteSync(long blogId) {
        CompletableFuture.runAsync(() -> syncBlogFromGit(blogId));
    }

    public void scheduleExportPost(long postId) {
        CompletableFuture.runAsync(() -> exportPostWithNewTransaction(postId));
    }

    private void syncBlogFromGit(long blogId) {
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                try {
                    syncBlogFromGitTransactional(blogId);
                } catch (Exception e) {
                    throw new IllegalStateException("Git pull/import failed blogId=%d".formatted(blogId), e);
                }
            });
        } catch (RuntimeException e) {
            LOG.error("Git import failed blogId={}", blogId, e);
        }
    }

    private void syncBlogFromGitTransactional(long blogId) throws Exception {
        Optional<Blog> blogOpt = blogRepository.findById(blogId);
        if (blogOpt.isEmpty()) {
            return;
        }
        Blog blog = blogOpt.get();
        if (!blog.isActive() || !isConfiguredForGit(blog)) {
            return;
        }
        Path workspace = workspaceForBlog(blogId);
        if (!prepareWorkspace(blog, workspace)) {
            return;
        }
        JekyllLayoutConvention convention = loadConvention(workspace);
        walkImport(blogId, convention.resolvePosts(workspace), BlogGitImportService.SourceKind.POSTS_FOLDER);
        walkImport(blogId, convention.resolveDrafts(workspace), BlogGitImportService.SourceKind.DRAFTS_FOLDER);

        String head = resolveHead(workspace);
        blog.setGitLastKnownCommit(head);
        blogRepository.save(blog);
    }

    private void walkImport(long blogId, Path root, BlogGitImportService.SourceKind kind) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(p -> Files.isRegularFile(p) && isMarkdown(p)).forEach(markdown -> {
                try {
                    blogGitImportService.ingest(blogId, markdown, kind);
                } catch (Exception ex) {
                    LOG.warn("Import failed markdown={}: {}", markdown, ex.toString());
                }
            });
        }
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
