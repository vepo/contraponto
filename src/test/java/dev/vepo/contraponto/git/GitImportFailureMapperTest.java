package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class GitImportFailureMapperTest {

    @Inject
    GitImportFailureMapper mapper;

    @Test
    void classifiesDescriptionLengthViolations() {
        var classified = mapper.classify(new RuntimeException(
                                                              "ERROR: value too long for type character varying(512)"));
        assertThat(classified.message()).contains("Description too long");
        assertThat(classified.remediation()).contains("512");
    }

    @Test
    void classifiesGenericRootMessage() {
        var classified = mapper.classify(new RuntimeException("Broken layout in _posts"));
        assertThat(classified.message()).contains("Could not import");
        assertThat(classified.remediation()).contains("Broken layout");
    }

    @Test
    void classifiesGenericWhenRootMessageBlank() {
        var classified = mapper.classify(new RuntimeException());
        assertThat(classified.message()).contains("Could not import");
        assertThat(classified.remediation()).contains("RuntimeException");
    }

    @Test
    void classifiesNullAsUnknown() {
        var classified = mapper.classify(null);
        assertThat(classified.message()).contains("Could not import");
        assertThat(classified.remediation()).contains("Unknown error");
    }

    @Test
    void classifiesSlugConflictFromDuplicateKey() {
        var classified = mapper.classify(new RuntimeException(
                                                              "duplicate key value violates unique constraint on tb_posts slug"));
        assertThat(classified.message()).contains("Slug already used");
    }

    @Test
    void classifiesSlugConflicts() {
        var classified = mapper.classify(new RuntimeException(
                                                              "duplicate key value violates unique constraint \"uk_posts_slug_user\""));
        assertThat(classified.message()).contains("Slug already used");
    }

    @Test
    void classifiesYamlFromMessageChain() {
        var classified = mapper.classify(new RuntimeException(
                                                              new RuntimeException("error while parsing --- front matter")));
        assertThat(classified.message()).contains("YAML");
    }

    @Test
    void classifiesYamlParseFailures() {
        var classified = mapper.classify(new JsonParseException(null, "Unexpected character"));
        assertThat(classified.message()).contains("YAML");
    }
}
