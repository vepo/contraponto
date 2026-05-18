package dev.vepo.contraponto.tag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TagSlugTest {

    @Test
    void hasInvalidSlugCharactersAcceptsValidSlug() {
        assertThat(TagSlug.hasInvalidSlugCharacters("my-tag-42")).isFalse();
    }

    @Test
    void hasInvalidSlugCharactersRejectsUnderscoreAndSpaces() {
        assertThat(TagSlug.hasInvalidSlugCharacters("my_tag")).isTrue();
        assertThat(TagSlug.hasInvalidSlugCharacters("my tag")).isTrue();
    }

    @Test
    void hasInvalidSlugCharactersRejectsUppercase() {
        assertThat(TagSlug.hasInvalidSlugCharacters("My-Tag")).isTrue();
    }

    @Test
    void hasInvalidSlugCharactersReturnsFalseForNull() {
        assertThat(TagSlug.hasInvalidSlugCharacters(null)).isFalse();
    }

    @Test
    void slugifyCollapsesRepeatedHyphens() {
        assertThat(TagSlug.slugify("a---b")).isEqualTo("a-b");
    }

    @Test
    void slugifyLowercasesTrimsAndHyphenatesWords() {
        assertThat(TagSlug.slugify("  My Tutorial  ")).isEqualTo("my-tutorial");
    }

    @Test
    void slugifyNormalizesAccentedCharacters() {
        assertThat(TagSlug.slugify("Über Café")).isEqualTo("ber-caf");
    }

    @Test
    void slugifyReplacesInvalidCharactersWithHyphens() {
        assertThat(TagSlug.slugify("hello@world!")).isEqualTo("hello-world");
    }

    @Test
    void slugifyReturnsEmptyForNull() {
        assertThat(TagSlug.slugify(null)).isEmpty();
    }

    @Test
    void slugifyStripsLeadingAndTrailingHyphens() {
        assertThat(TagSlug.slugify("--hello--")).isEqualTo("hello");
    }

    @Test
    void slugifyTruncatesTo255CharactersAndStripsTrailingHyphen() {
        String raw = "a".repeat(254) + "-tail";
        String slug = TagSlug.slugify(raw);

        assertThat(slug).hasSize(254)
                        .isEqualTo("a".repeat(254))
                        .doesNotEndWith("-");
    }
}
