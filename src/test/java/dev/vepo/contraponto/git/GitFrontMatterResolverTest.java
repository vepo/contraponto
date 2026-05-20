package dev.vepo.contraponto.git;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.renderer.Format;

@UnitTest
class GitFrontMatterResolverTest {

    @Test
    void resolveCoverPathFallsBackToImage() {
        Map<String, Object> fm = Map.of("image", "/assets/images/capas/x.webp");
        assertThat(GitFrontMatterResolver.resolveCoverPath(fm)).isEqualTo("/assets/images/capas/x.webp");
    }

    @Test
    void resolveCoverPathPrefersCoverOverImage() {
        Map<String, Object> fm = new LinkedHashMap<>();
        fm.put("cover", "/assets/images/a.png");
        fm.put("image", "/assets/images/b.png");
        assertThat(GitFrontMatterResolver.resolveCoverPath(fm)).isEqualTo("/assets/images/a.png");
    }

    @Test
    void resolveFormatFromAdocExtension() {
        Map<String, Object> fm = Map.of();
        Path file = Path.of("2020-01-01-note.adoc");
        assertThat(GitFrontMatterResolver.resolveFormat(fm, file)).isEqualTo(Format.ASCIIDOC);
    }

    @Test
    void resolvePublishedAtPrefersPublishedAtThenPublishDate() {
        Map<String, Object> fm = new LinkedHashMap<>();
        fm.put("publish_date", "2023-09-26 15:39:23 +0300");
        LocalDateTime parsed = GitFrontMatterResolver.resolvePublishedAt(fm, Optional.of(LocalDate.of(2020, 1, 1)));
        assertThat(parsed).isEqualTo(LocalDateTime.of(2023, 9, 26, 15, 39, 23));
    }

    @Test
    void resolveSerieTitleFallsBackToSeries() {
        Map<String, Object> fm = Map.of("series", "Conversas sobre Arquitetura");
        assertThat(GitFrontMatterResolver.resolveSerieTitle(fm)).isEqualTo("Conversas sobre Arquitetura");
    }

    @Test
    void resolveSerieTitlePrefersSerieOverSeries() {
        Map<String, Object> fm = new LinkedHashMap<>();
        fm.put("serie", "Native Serie");
        fm.put("series", "Jekyll Series");
        assertThat(GitFrontMatterResolver.resolveSerieTitle(fm)).isEqualTo("Native Serie");
    }

    @Test
    void resolveSlugPrefersExplicitSlugOverPermalink() {
        Map<String, Object> fm = Map.of(
                                        "slug", "Explicit",
                                        "permalink", "/posts/other");
        assertThat(GitFrontMatterResolver.resolveSlug(fm, "stem")).isEqualTo("explicit");
    }

    @Test
    void resolveSlugUsesPermalinkLastSegment() {
        Map<String, Object> fm = Map.of("permalink", "/posts/sobre-design-de-codigo");
        assertThat(GitFrontMatterResolver.resolveSlug(fm, "00-00-00-wrong")).isEqualTo("sobre-design-de-codigo");
    }

    @Test
    void slugFromPermalinkHandlesTrailingSlashAndHtml() {
        assertThat(GitFrontMatterResolver.slugFromPermalink("/posts/foo/")).isEqualTo("foo");
        assertThat(GitFrontMatterResolver.slugFromPermalink("/posts/bar.html")).isEqualTo("bar");
    }
}
