package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.image.Image;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class GitImageSyncServiceTest {

    private static final String UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Inject
    GitImageSyncService gitImageSyncService;

    private User author;

    private Blog blog;
    private Image image;

    @Test
    void addCoverFrontMatterFallsBackToLivePublicationCover() {
        var post = new Post();
        var live = new PostPublication();
        live.setCover(image);
        post.setLivePublication(live);
        var fm = new LinkedHashMap<String, Object>();
        gitImageSyncService.addCoverFrontMatter(fm, post, JekyllLayoutConvention.defaults());
        assertThat(fm.get("cover")).isEqualTo("assets/images/" + image.getUuid() + ".png");
    }

    @Test
    void addCoverFrontMatterUsesPostCover() {
        var post = new Post();
        post.setCover(image);
        var fm = new LinkedHashMap<String, Object>();
        gitImageSyncService.addCoverFrontMatter(fm, post, JekyllLayoutConvention.defaults());
        assertThat(fm.get("cover")).isEqualTo("assets/images/" + image.getUuid() + ".png");
    }

    @Test
    void importAssetsFromWorkspaceMapsLongJekyllFilenames() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-long-name");
        var convention = JekyllLayoutConvention.defaults();
        Path assetsDir = convention.resolveAssets(workspace);
        Files.createDirectories(assetsDir);
        String jekyllName = "kafka-distributed-systems-architecture-diagram";
        Files.write(assetsDir.resolve(jekyllName + ".png"), new byte[] { 9, 8, 7 });

        gitImageSyncService.importAssetsFromWorkspace(blog, workspace, convention);

        String uuid = GitImportedAssetId.normalize(jekyllName, ".png");
        assertThat(Given.inject(dev.vepo.contraponto.image.ImageRepository.class).findByUuid(uuid)).isPresent();
    }

    @Test
    void importAssetsFromWorkspaceSkipsMissingDirectory() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-no-assets");
        gitImageSyncService.importAssetsFromWorkspace(blog, workspace, JekyllLayoutConvention.defaults());
        assertThat(Given.inject(dev.vepo.contraponto.image.ImageRepository.class).findByUuid(UUID)).isEmpty();
    }

    @Test
    void importAssetsFromWorkspaceWalksNestedDirectories() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-nested-walk");
        var convention = JekyllLayoutConvention.defaults();
        Path nested = convention.resolveAssets(workspace).resolve("capas");
        Files.createDirectories(nested);
        String jekyllName = "walk-nested";
        Files.write(nested.resolve(jekyllName + ".png"), new byte[] { 3, 4 });

        gitImageSyncService.importAssetsFromWorkspace(blog, workspace, convention);

        String uuid = GitImportedAssetId.normalize(jekyllName, ".png");
        assertThat(Given.inject(dev.vepo.contraponto.image.ImageRepository.class).findByUuid(uuid)).isPresent();
    }

    @Test
    void importImagesForPostImportsAsciiDocMacroPaths() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-adoc-macro");
        var convention = JekyllLayoutConvention.defaults();
        Path nested = convention.resolveAssets(workspace).resolve("java-101/cap-01");
        Files.createDirectories(nested);
        Files.write(nested.resolve("diagram.PNG"), new byte[] { 1, 2, 3 });

        String body = "image::java-101/cap-01/diagram.PNG[id=diagram, align=\"center\"]";
        gitImageSyncService.importImagesForPost(blog, workspace, convention, null, body, Format.ASCIIDOC);

        String uuid = GitImportedAssetId.normalize("diagram", ".PNG");
        assertThat(Given.inject(dev.vepo.contraponto.image.ImageRepository.class).findByUuid(uuid)).isPresent();
    }

    @Test
    void prepareBodyForExportReturnsEmptyStringForNullBody() {
        assertThat(gitImageSyncService.prepareBodyForExport(null, JekyllLayoutConvention.defaults())).isEmpty();
    }

    @Test
    void prepareBodyForExportRewritesApiImageUrls() {
        String body = "![diagram](/api/images/" + image.getUuid() + ".png)";
        String exported = gitImageSyncService.prepareBodyForExport(body, JekyllLayoutConvention.defaults());
        assertThat(exported).contains("assets/images/" + image.getUuid() + ".png");
        assertThat(exported).doesNotContain("/api/images/");
    }

    @Test
    void prepareBodyForExportUsesStoredGitAssetPath() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-export-path");
        var convention = JekyllLayoutConvention.defaults();
        Path nested = convention.resolveAssets(workspace).resolve("capas");
        Files.createDirectories(nested);
        String jekyllName = "nested-cover";
        Files.write(nested.resolve(jekyllName + ".webp"), new byte[] { 7, 8, 9 });

        String markdown = "![cap](assets/images/capas/" + jekyllName + ".webp)";
        String stored = gitImageSyncService.prepareBodyForImport(markdown, blog, workspace, convention, Format.MARKDOWN);

        String uuid = GitImportedAssetId.normalize(jekyllName, ".webp");
        var imported = Given.inject(dev.vepo.contraponto.image.ImageRepository.class).findByUuid(uuid).orElseThrow();
        assertThat(imported.getGitAssetRelativePath()).isEqualTo("capas/" + jekyllName);

        String exported = gitImageSyncService.prepareBodyForExport(stored, convention);
        assertThat(exported).contains("assets/images/capas/" + jekyllName + ".webp");
        assertThat(exported).doesNotContain(uuid + ".webp");
    }

    @Test
    void prepareBodyForImportPreservesAsciiDocBlockTitleBeforeImage() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-adoc-block-title");
        var convention = JekyllLayoutConvention.defaults();
        Path nested = convention.resolveAssets(workspace).resolve("databases");
        Files.createDirectories(nested);
        Files.write(nested.resolve("storage-types.png"), new byte[] { 1, 2, 3 });

        String caption = "Tipos de armazenamentos possíveis para bases de dados";
        String body = """
                      .%s
                      image::databases/storage-types.png[]
                      """.formatted(caption);
        String stored = gitImageSyncService.prepareBodyForImport(body, blog, workspace, convention, Format.ASCIIDOC);

        String uuid = GitImportedAssetId.normalize("storage-types", ".png");
        assertThat(stored).contains("." + caption);
        assertThat(stored).contains("image::/api/images/" + uuid + ".png[]");
        assertThat(stored).contains("<!-- contraponto:image uuid=\"" + uuid + "\" -->");

        String[] lines = stored.split("\n");
        int markerIndex = -1;
        int imageIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("contraponto:image")) {
                markerIndex = i;
            }
            if (lines[i].startsWith("image::/api/images/")) {
                imageIndex = i;
            }
        }
        assertThat(markerIndex).isGreaterThanOrEqualTo(0);
        assertThat(imageIndex).isEqualTo(markerIndex + 1);

        assertThat(Given.inject(dev.vepo.contraponto.image.ImageRepository.class).findByUuid(uuid)).isPresent();
    }

    @Test
    void prepareBodyForImportRegistersAssetsAndRewritesMarkdown() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-import");
        var convention = JekyllLayoutConvention.defaults();
        Path assetsDir = convention.resolveAssets(workspace);
        Files.createDirectories(assetsDir);
        String assetName = UUID + ".png";
        Files.write(assetsDir.resolve(assetName), new byte[] { 1, 2, 3 });

        String markdown = "![pic](assets/images/" + UUID + ".png)";
        String stored = gitImageSyncService.prepareBodyForImport(markdown, blog, workspace, convention, Format.MARKDOWN);

        assertThat(stored).contains("/api/images/" + UUID + ".png");
        assertThat(stored).contains("contraponto:image");
    }

    @Test
    void prepareBodyForImportReturnsEmptyStringForNullBody() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-null");
        assertThat(gitImageSyncService.prepareBodyForImport(null, blog, workspace, JekyllLayoutConvention.defaults(), Format.MARKDOWN))
                                                                                                                                       .isEmpty();
    }

    @Test
    void prepareBodyForImportRewritesAsciiDocImageMacro() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-adoc-rewrite");
        var convention = JekyllLayoutConvention.defaults();
        Path nested = convention.resolveAssets(workspace).resolve("java-101/cap-01");
        Files.createDirectories(nested);
        Files.write(nested.resolve("diagram.png"), new byte[] { 1, 2 });

        String body = "image::java-101/cap-01/diagram.png[Caption]";
        String stored = gitImageSyncService.prepareBodyForImport(body, blog, workspace, convention, Format.ASCIIDOC);

        String uuid = GitImportedAssetId.normalize("diagram", ".png");
        assertThat(stored).contains("/api/images/" + uuid + ".png");
    }

    @Test
    void prepareBodyForImportRewritesLongJekyllAssetPaths() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-long-md");
        var convention = JekyllLayoutConvention.defaults();
        Path assetsDir = convention.resolveAssets(workspace);
        Files.createDirectories(assetsDir);
        String jekyllName = "legacy-screenshot-from-vepo-github-io";
        Files.write(assetsDir.resolve(jekyllName + ".jpg"), new byte[] { 4, 5, 6 });

        String markdown = "![diagram](assets/images/" + jekyllName + ".jpg)";
        String stored = gitImageSyncService.prepareBodyForImport(markdown, blog, workspace, convention, Format.MARKDOWN);

        String uuid = GitImportedAssetId.normalize(jekyllName, ".jpg");
        assertThat(stored).contains("/api/images/" + uuid + ".jpg");
    }

    @Test
    void prepareBodyForImportRewritesMarkdownWithLeadingSlashBeforeAssets() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-md-leading-slash");
        var convention = JekyllLayoutConvention.defaults();
        Path nested = convention.resolveAssets(workspace).resolve("conversas-sobre-arquitetura");
        Files.createDirectories(nested);
        Files.write(nested.resolve("fig-05-atam-steps.png"), new byte[] { 1, 2, 3 });

        String markdown = "![Etapas do ATAM](/assets/images/conversas-sobre-arquitetura/fig-05-atam-steps.png)";
        String stored = gitImageSyncService.prepareBodyForImport(markdown, blog, workspace, convention, Format.MARKDOWN);

        String uuid = GitImportedAssetId.normalize("fig-05-atam-steps", ".png");
        assertThat(stored).contains("(/api/images/" + uuid + ".png)");
        assertThat(stored).doesNotContain("//api/images/");
    }

    @Test
    void prepareBodyForImportRewritesNestedAssetPaths() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-nested");
        var convention = JekyllLayoutConvention.defaults();
        Path nested = convention.resolveAssets(workspace).resolve("capas");
        Files.createDirectories(nested);
        String jekyllName = "nested-cover";
        Files.write(nested.resolve(jekyllName + ".webp"), new byte[] { 7, 8, 9 });

        String markdown = "![cap](assets/images/capas/" + jekyllName + ".webp)";
        String stored = gitImageSyncService.prepareBodyForImport(markdown, blog, workspace, convention, Format.MARKDOWN);

        String uuid = GitImportedAssetId.normalize(jekyllName, ".webp");
        assertThat(stored).contains("/api/images/" + uuid + ".webp");
    }

    @Test
    void relativeAssetPatternMatchesRelativePaths() {
        var convention = JekyllLayoutConvention.defaults();
        var pattern = GitImageSyncService.relativeAssetPattern(convention);
        assertThat(pattern.matcher("![](assets/images/" + UUID + ".png)").find()).isTrue();
        assertThat(pattern.matcher("![](../assets/images/" + UUID + ".webp)").find()).isTrue();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        author = Given.user()
                      .withUsername("gitimg")
                      .withEmail("gitimg@test.com")
                      .withName("Git Image")
                      .withPassword("Password123!")
                      .persist();
        blog = author.getDefaultBlog();
        image = Given.randomCover(blog);
    }
}
