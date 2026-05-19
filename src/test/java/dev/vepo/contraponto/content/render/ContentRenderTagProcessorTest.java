package dev.vepo.contraponto.content.render;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ContentRenderTagProcessorTest {

    @Inject
    ContentRenderTagProcessor processor;

    @Test
    void gistTagAcceptsFullUrl() {
        String url = "https://gist.github.com/vepo/b63ff8384941329485266999f99e2264";
        String input = "{% gist " + url + " %}";
        assertThat(processor.apply(input)).contains("gist.github.com/vepo/b63ff8384941329485266999f99e2264.js");
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
