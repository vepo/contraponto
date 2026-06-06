package dev.vepo.contraponto.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.renderer.Format;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import dev.vepo.contraponto.shared.TestTimes;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class PostChangeDiffServiceTest {

    private static PostPublication publication(int version, String title, String content) {
        PostPublication p = new PostPublication();
        p.setVersion(version);
        p.setTitle(title);
        p.setDescription("desc");
        p.setContent(content);
        p.setFormat(Format.MARKDOWN);
        p.setSlug("slug");
        p.setPublishedAt(TestTimes.REFERENCE);
        return p;
    }

    @Inject
    PostChangeDiffService diffService;

    @Test
    void buildVersionDiffs_marks_first_version_without_diff() {
        PostPublication v1 = publication(1, "A", "content");
        PostPublication v2 = publication(2, "B", "content changed");

        List<PostChangeDiffService.VersionDiff> versions =
                diffService.buildVersionDiffs(List.of(v2, v1));

        assertThat(versions).hasSize(2);
        assertThat(versions.get(1).isFirstVersion()).isTrue();
        assertThat(versions.get(0).diffFromPrevious().hasChanges()).isTrue();
    }

    @Test
    void diff_detects_title_and_content_changes() {
        PostPublication older = publication(1, "Old title", "line one\nline two");
        PostPublication newer = publication(2, "New title", "line one\nline three");

        var diff = diffService.diff(older, newer);

        assertThat(diff.titleChanged()).isTrue();
        assertThat(diff.contentChanged()).isTrue();
        assertThat(diff.contentDiffHtml()).contains("post-history__diff-del");
        assertThat(diff.contentDiffHtml()).contains("post-history__diff-ins");
    }
}
