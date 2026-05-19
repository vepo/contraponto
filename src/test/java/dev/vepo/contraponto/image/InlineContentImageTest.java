package dev.vepo.contraponto.image;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.git.GitImageSyncService;
import dev.vepo.contraponto.git.JekyllLayoutConvention;
import dev.vepo.contraponto.custompage.PagePlacement;
import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.user.User;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class InlineContentImageTest {

    private static final String BLOCK_TITLE_CAPTION = "Tipos de armazenamentos possíveis para bases de dados";

    @TestHTTPResource("/")
    URL baseUrl;

    @Inject
    CustomPageImageDependencyService customPageImageDependencyService;

    @Inject
    GitImageSyncService gitImageSyncService;

    private User author;
    private Blog blog;

    private void assertInlineImageShown(String html, Image image) {
        assertThat(html).contains("src=\"" + image.getUrl() + "\"");
        given().get(image.getUrl())
               .then()
               .statusCode(200)
               .contentType(containsString("image/"));
    }

    @Test
    void customPagePreservesInlineImageWithResolvableUrl() {
        var image = Given.randomCover(blog);
        var html = "<p><img src=\"%s\" alt=\"caption\"></p>".formatted(image.getUrl());
        var page = Given.customPage()
                        .withSlug("/inline-image-page")
                        .withTitle("Inline image page")
                        .withContent(html)
                        .withPlacement(PagePlacement.FOOTER)
                        .withSection("Test")
                        .persist();

        var stored = customPageImageDependencyService.normalizeAndStoreContent(page, html);
        assertInlineImageShown(stored, image);

        given().get(CustomPagePaths.publicUrl(page))
               .then()
               .statusCode(200)
               .body(containsString("src=\"" + image.getUrl() + "\""));
    }

    @Test
    void publishedAsciiDocBlockTitleImageRendersWithCaption() throws IOException {
        Path workspace = Files.createTempDirectory("inline-adoc-block-title");
        var convention = JekyllLayoutConvention.defaults();
        Path nested = convention.resolveAssets(workspace).resolve("databases");
        Files.createDirectories(nested);
        Files.write(nested.resolve("storage-types.png"), new byte[] { 1, 2, 3 });

        String body = """
                      .%s
                      image::databases/storage-types.png[]
                      """.formatted(BLOCK_TITLE_CAPTION);
        String stored = gitImageSyncService.prepareBodyForImport(body, blog, workspace, convention, Format.ASCIIDOC);

        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(blog)
                        .withTitle("AsciiDoc block title image")
                        .withSlug("asciidoc-block-title-image")
                        .withFormat(Format.ASCIIDOC)
                        .withContent(stored)
                        .withPublished(true)
                        .persist();

        String html = TemplateExtensions.render(post);
        assertThat(html).contains(BLOCK_TITLE_CAPTION);
        assertThat(html).doesNotContain("contraponto:image");
        assertThat(html).doesNotContain("image::");
        String imageUrl = stored.lines()
                                .filter(line -> line.startsWith("image::/api/images/"))
                                .findFirst()
                                .orElseThrow()
                                .replace("image::", "")
                                .replace("[]", "");
        given().get(imageUrl)
               .then()
               .statusCode(200)
               .contentType(containsString("image/"));
        assertThat(html).contains("src=\"" + imageUrl + "\"");
    }

    @Test
    void publishedAsciiDocPostRendersInlineImageWithResolvableUrl() {
        var image = Given.randomCover(blog);
        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(blog)
                        .withTitle("AsciiDoc with image")
                        .withSlug("asciidoc-with-image")
                        .withFormat(Format.ASCIIDOC)
                        .withContent("image::%s[caption]".formatted(image.getUrl()))
                        .withPublished(true)
                        .persist();

        assertInlineImageShown(TemplateExtensions.render(post), image);
    }

    @Test
    void publishedMarkdownPostRendersInlineImageWithResolvableUrl() {
        var image = Given.randomCover(blog);
        var post = Given.post()
                        .withAuthor(author)
                        .withBlog(blog)
                        .withTitle("Markdown with image")
                        .withSlug("markdown-with-image")
                        .withContent("![caption](%s)".formatted(image.getUrl()))
                        .withPublished(true)
                        .persist();

        assertInlineImageShown(TemplateExtensions.render(post), image);
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        io.restassured.RestAssured.baseURI = baseUrl.getProtocol() + "://" + baseUrl.getHost();
        io.restassured.RestAssured.port = baseUrl.getPort();
        io.restassured.RestAssured.basePath = "";

        author = Given.user()
                      .withUsername("inlineimg")
                      .withEmail("inlineimg@test.com")
                      .withName("Inline Image Author")
                      .withPassword("Password123!")
                      .persist();
        blog = author.getDefaultBlog();
    }
}
