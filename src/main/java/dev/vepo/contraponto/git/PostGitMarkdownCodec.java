package dev.vepo.contraponto.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

@ApplicationScoped
public final class PostGitMarkdownCodec {

    public record ParsedFrontMatterMarkdown(Map<String, Object> frontMatter, String body) {}

    /** Jekyll-style published post filename stem after the date segment. */
    public static final Pattern PUBLISHED_POST_FILENAME =
            Pattern.compile("^(?<date>\\d{4}-\\d{2}-\\d{2})-(?<slug>.+)\\.(md|markdown)$", Pattern.CASE_INSENSITIVE);

    private static final ObjectMapper YAML_MAP = yamlMapper();

    private static ObjectMapper yamlMapper() {
        YAMLFactory factory = YAMLFactory.builder()
                                         .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                                         .build();
        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.findAndRegisterModules();
        return mapper;
    }

    public ParsedFrontMatterMarkdown parseMarkdownDocument(String markdown) throws IOException {
        if (markdown == null) {
            return new ParsedFrontMatterMarkdown(Map.of(), "");
        }
        if (!markdown.startsWith("---")) {
            return new ParsedFrontMatterMarkdown(Map.of(), markdown);
        }
        int closing = markdown.indexOf("\n---", 3);
        if (closing < 0) {
            return new ParsedFrontMatterMarkdown(Map.of(), markdown);
        }
        String fm = markdown.substring(3, closing).strip();
        String bodyStart = markdown.substring(closing + 4); // newline after "---"
        if (bodyStart.startsWith("\n")) {
            bodyStart = bodyStart.substring(1);
        } else if (bodyStart.startsWith("\r\n")) {
            bodyStart = bodyStart.substring(2);
        }

        Map<String, Object> map =
                YAML_MAP.readValue(fm, new TypeReference<>() {});
        if (map == null) {
            map = Map.of();
        }
        return new ParsedFrontMatterMarkdown(map, bodyStart);
    }

    public Map<String, Object> readYamlObjectMap(Path yamlFile) throws IOException {
        return YAML_MAP.readValue(yamlFile.toFile(), new TypeReference<>() {});
    }

    public String writeMarkdownDocument(Map<String, Object> frontMatter, String body) throws IOException {
        String fmYaml = YAML_MAP.writeValueAsString(frontMatter);
        String separator = fmYaml.endsWith("\n") ? "" : "\n";
        return "---\n%s%s---\n\n%s".formatted(fmYaml, separator, body);
    }

}
