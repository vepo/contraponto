package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
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
    void classifiesSlugConflicts() {
        var classified = mapper.classify(new RuntimeException(
                                                              "duplicate key value violates unique constraint \"uk_posts_slug_user\""));
        assertThat(classified.message()).contains("Slug already used");
    }

    @Test
    void classifiesYamlParseFailures() {
        var classified = mapper.classify(new JsonParseException(null, "Unexpected character"));
        assertThat(classified.message()).contains("YAML");
    }
}
