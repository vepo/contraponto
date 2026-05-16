package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PostGitMarkdownCodecTest {

    private final PostGitMarkdownCodec codec = new PostGitMarkdownCodec();

    @Test
    void parseExtractsMinimalYamlMaps() throws Exception {
        PostGitMarkdownCodec.ParsedFrontMatterMarkdown emptyKeys = codec.parseMarkdownDocument("""
                                                                                               ---
                                                                                               z: []
                                                                                               ---


                                                                                               trailer""");
        assertThat((List<?>) emptyKeys.frontMatter().get("z")).isEmpty();
        assertThat(emptyKeys.body().strip()).isEqualTo("trailer");

        PostGitMarkdownCodec.ParsedFrontMatterMarkdown sane = codec.parseMarkdownDocument("""
                                                                                          ---
                                                                                          x: 1
                                                                                          ---

                                                                                          ok""");
        assertThat(sane.frontMatter().get("x")).isEqualTo(1);
        assertThat(sane.body().strip()).isEqualTo("ok");
    }

    @Test
    void parseExtractsYamlAndUnixOrWindowsBodyPrefix() throws Exception {
        PostGitMarkdownCodec.ParsedFrontMatterMarkdown unix = codec.parseMarkdownDocument("---\ntitle: T\n---\n\nLine1");
        assertThat(unix.frontMatter().get("title")).isEqualTo("T");
        assertThat(unix.body().strip()).isEqualTo("Line1");

        PostGitMarkdownCodec.ParsedFrontMatterMarkdown crlf = codec.parseMarkdownDocument("---\r\ntitle: X\r\n---\r\n\r\nHey");
        assertThat(crlf.frontMatter().get("title")).isEqualTo("X");
        assertThat(crlf.body().strip()).isEqualTo("Hey");
    }

    @Test
    void parseReturnsBodyWhenClosingDelimiterMissing() throws Exception {
        String raw = "---\ntitle: A\nBroken block";
        PostGitMarkdownCodec.ParsedFrontMatterMarkdown doc = codec.parseMarkdownDocument(raw);
        assertThat(doc.frontMatter()).isEmpty();
        assertThat(doc.body()).isEqualTo(raw);
    }

    @Test
    void parseReturnsEmptyFrontMatterForNullOrNoFrontMatterBlock() throws Exception {
        PostGitMarkdownCodec.ParsedFrontMatterMarkdown docNull = codec.parseMarkdownDocument(null);
        assertThat(docNull.frontMatter()).isEmpty();
        assertThat(docNull.body()).isEmpty();

        String bodyOnly = "= Title\nHello";
        PostGitMarkdownCodec.ParsedFrontMatterMarkdown docPlain = codec.parseMarkdownDocument(bodyOnly);
        assertThat(docPlain.frontMatter()).isEmpty();
        assertThat(docPlain.body()).isEqualTo(bodyOnly);
    }

    @Test
    void publishedPostFilenamePatternMatchesCaseInsensitiveExtension() {
        var m = PostGitMarkdownCodec.PUBLISHED_POST_FILENAME.matcher("2026-05-15-hello.MARKDOWN");
        assertThat(m.matches()).isTrue();
        assertThat(m.group("date")).isEqualTo("2026-05-15");
        assertThat(m.group("slug")).isEqualTo("hello");
    }

    @Test
    void readYamlObjectMapReadsFile(@TempDir Path dir) throws Exception {
        Path yml = dir.resolve("sample.yml");
        Files.writeString(yml, "a: 1\nb: two\n", StandardCharsets.UTF_8);
        LinkedHashMap<String, Object> map = codec.readYamlObjectMap(yml);
        assertThat(map.get("a")).isEqualTo(1);
        assertThat(map.get("b")).isEqualTo("two");
    }

    @Test
    void writeMarkdownProducesDelimitedFrontMatter() throws Exception {
        LinkedHashMap<String, Object> fm = new LinkedHashMap<>(Map.of("k", "v"));
        String md = codec.writeMarkdownDocument(fm, "text");
        assertThat(md).startsWith("---\n").contains("k: \"v\"", "text");
    }
}
