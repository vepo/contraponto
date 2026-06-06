package dev.vepo.contraponto.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@UnitTest
class SlugTest {

    @Test
    void hasInvalidSlugCharactersAcceptsValidSlug() {
        assertThat(Slug.hasInvalidSlugCharacters("my-tag-42")).isFalse();
    }

    @Test
    void hasInvalidSlugCharactersRejectsUnderscoreAndSpaces() {
        assertThat(Slug.hasInvalidSlugCharacters("my_tag")).isTrue();
        assertThat(Slug.hasInvalidSlugCharacters("my tag")).isTrue();
    }

    @Test
    void hasInvalidSlugCharactersRejectsUppercase() {
        assertThat(Slug.hasInvalidSlugCharacters("My-Tag")).isTrue();
    }

    @Test
    void hasInvalidSlugCharactersReturnsFalseForNull() {
        assertThat(Slug.hasInvalidSlugCharacters(null)).isFalse();
    }

    @Test
    void slugifyCollapsesRepeatedHyphens() {
        assertThat(Slug.slugify("a---b")).isEqualTo("a-b");
    }

    @Test
    void slugifyLowercasesTrimsAndHyphenatesWords() {
        assertThat(Slug.slugify("  My Tutorial  ")).isEqualTo("my-tutorial");
    }

    @Test
    void slugifyReplacesInvalidCharactersWithHyphens() {
        assertThat(Slug.slugify("hello@world!")).isEqualTo("hello-world");
    }

    @Test
    void slugifyReturnsEmptyForNull() {
        assertThat(Slug.slugify(null)).isEmpty();
    }

    @Test
    void slugifyStripsLeadingAndTrailingHyphens() {
        assertThat(Slug.slugify("--hello--")).isEqualTo("hello");
    }

    @Test
    void slugifyTransliteratesAccentedLatinCharacters() {
        assertThat(Slug.slugify("Über Café")).isEqualTo("uber-cafe");
        assertThat(Slug.slugify("São Paulo")).isEqualTo("sao-paulo");
        assertThat(Slug.slugify("Coração")).isEqualTo("coracao");
    }

    @Test
    void slugifyTruncatesTo255CharactersAndStripsTrailingHyphen() {
        String raw = "a".repeat(254) + "-tail";
        String slug = Slug.slugify(raw);

        assertThat(slug).hasSize(254)
                        .isEqualTo("a".repeat(254))
                        .doesNotEndWith("-");
    }
}
