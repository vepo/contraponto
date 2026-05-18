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
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
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
    void importAssetsFromWorkspaceSkipsMissingDirectory() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-no-assets");
        gitImageSyncService.importAssetsFromWorkspace(blog, workspace, JekyllLayoutConvention.defaults());
        assertThat(Given.inject(dev.vepo.contraponto.image.ImageRepository.class).findByUuid(UUID)).isEmpty();
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
    void prepareBodyForImportRegistersAssetsAndRewritesMarkdown() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-import");
        var convention = JekyllLayoutConvention.defaults();
        Path assetsDir = convention.resolveAssets(workspace);
        Files.createDirectories(assetsDir);
        String assetName = UUID + ".png";
        Files.write(assetsDir.resolve(assetName), new byte[] { 1, 2, 3 });

        String markdown = "![pic](assets/images/" + UUID + ".png)";
        String stored = gitImageSyncService.prepareBodyForImport(markdown, blog, workspace, convention);

        assertThat(stored).contains("/api/images/" + UUID + ".png");
        assertThat(stored).contains("contraponto:image");
    }

    @Test
    void prepareBodyForImportReturnsEmptyStringForNullBody() throws IOException {
        Path workspace = Files.createTempDirectory("git-image-null");
        assertThat(gitImageSyncService.prepareBodyForImport(null, blog, workspace, JekyllLayoutConvention.defaults()))
                                                                                                                      .isEmpty();
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
