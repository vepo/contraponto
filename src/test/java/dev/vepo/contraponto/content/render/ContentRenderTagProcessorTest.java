package dev.vepo.contraponto.content.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
class ContentRenderTagProcessorTest {

    @Inject
    ContentRenderTagProcessor processor;

    @Test
    void emptyContentIsUnchanged() {
        assertThat(processor.apply("")).isEmpty();
    }

    @Test
    void gistTagAcceptsFullUrl() {
        String url = "https://gist.github.com/vepo/b63ff8384941329485266999f99e2264";
        String input = "{% gist " + url + " %}";
        assertThat(processor.apply(input)).contains("gist.github.com/vepo/b63ff8384941329485266999f99e2264.js");
    }

    @Test
    void malformedTagIsLeftUntouched() {
        String input = "Before {% youtube dQw4w9WgXcQ after";
        assertThat(processor.apply(input)).isEqualTo(input);
    }

    @Test
    void multipleTagsInOneString() {
        String input = "{% youtube dQw4w9WgXcQ %} and {% youtube hPoHp0WhglA %}";
        String result = processor.apply(input);
        assertThat(result).contains("youtube.com/embed/dQw4w9WgXcQ").contains("youtube.com/embed/hPoHp0WhglA");
    }

    @Test
    void nullContentReturnsEmptyString() {
        assertThat(processor.apply(null)).isEmpty();
    }

    @Test
    void unknownTagIsPreserved() {
        String input = "Before {% unknown param %} after";
        assertThat(processor.apply(input)).isEqualTo(input);
    }

    @Test
    void youtubeTagIsReplaced() {
        String input = "Watch {% youtube dQw4w9WgXcQ %}";
        assertThat(processor.apply(input)).contains("youtube.com/embed/dQw4w9WgXcQ");
    }
}
