package dev.vepo.contraponto.git;

import dev.vepo.contraponto.shared.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

@UnitTest
class JekyllLayoutConventionTest {

    @Test
    void defaultsExposeStandardDirectories() {
        JekyllLayoutConvention d = JekyllLayoutConvention.defaults();
        assertThat(d.postsRelative()).isEqualTo("_posts");
        assertThat(d.draftsRelative()).isEqualTo("_drafts");
        assertThat(d.assetsRelative()).isEqualTo("assets/images");
        assertThat(JekyllLayoutConvention.locale()).isSameAs(Locale.ROOT);
        Path repoRoot = Path.of("fixture-blog-root").toAbsolutePath();
        assertThat(d.resolvePosts(repoRoot)).isEqualTo(repoRoot.resolve("_posts"));
        assertThat(d.resolveDrafts(repoRoot)).isEqualTo(repoRoot.resolve("_drafts"));
        assertThat(d.resolveAssets(repoRoot)).isEqualTo(repoRoot.resolve("assets/images"));
    }

    @Test
    void fromYamlAppliesCustomDirectoriesAndTrimsSlashes() {
        Map<String, Object> map = Map.of("posts_directory", "/pub/", "drafts_directory", "\\wip\\", "layout_fm_key",
                                         " template ", "default_layout", " article ");
        JekyllLayoutConvention c = JekyllLayoutConvention.fromYaml(map);
        assertThat(c.postsRelative()).isEqualTo("pub");
        assertThat(c.draftsRelative()).isEqualTo("wip");
        assertThat(c.layoutFrontMatterKey()).isEqualTo("template");
        assertThat(c.defaultLayoutValue()).isEqualTo("article");
    }

    @Test
    void fromYamlFallsBackToDefaultsForEmptyOrNullKeys() {
        assertThat(JekyllLayoutConvention.fromYaml(null)).extracting(JekyllLayoutConvention::postsRelative,
                                                                     JekyllLayoutConvention::draftsRelative)
                                                         .containsExactly("_posts", "_drafts");

        HashMap<String, Object> blanks = new HashMap<>();
        blanks.put("posts_directory", "");
        blanks.put("drafts_directory", " ");
        blanks.put("layout_fm_key", null);
        JekyllLayoutConvention c = JekyllLayoutConvention.fromYaml(blanks);
        assertThat(c.layoutFrontMatterKey()).isEqualTo(JekyllLayoutConvention.defaults().layoutFrontMatterKey());
        assertThat(c.defaultLayoutValue()).isEqualTo(JekyllLayoutConvention.defaults().defaultLayoutValue());
    }

    @Test
    void fromYamlRejectsParentSegments() {
        Map<String, Object> bad = Map.of("posts_directory", "legit/../hack");
        assertThatThrownBy(() -> JekyllLayoutConvention.fromYaml(bad)).isInstanceOf(IllegalArgumentException.class)
                                                                      .hasMessageContaining("..");
    }
}
