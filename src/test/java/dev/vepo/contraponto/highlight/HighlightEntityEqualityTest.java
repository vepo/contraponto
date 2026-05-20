package dev.vepo.contraponto.highlight;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class HighlightEntityEqualityTest {

    private static void setEntityId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void commonHighlightProposalEqualsUsesPersistedId() {
        var left = new CommonHighlightProposal();
        setEntityId(left, 9L);
        var right = new CommonHighlightProposal();
        setEntityId(right, 9L);
        assertThat(left).isEqualTo(right);
        assertThat(left).isNotEqualTo(new CommonHighlightProposal());
    }

    @Test
    void highlightNoteEqualsRejectsNullOrDifferentId() {
        var note = new HighlightNote();
        setEntityId(note, 3L);
        assertThat(note.equals(null)).isFalse();
        assertThat(note).isNotEqualTo(new HighlightNote());
    }

    @Test
    void highlightNoteEqualsUsesPersistedId() {
        var left = new HighlightNote();
        setEntityId(left, 5L);
        var right = new HighlightNote();
        setEntityId(right, 5L);
        assertThat(left).isEqualTo(right);
        assertThat(left).hasSameHashCodeAs(right);
    }

    @Test
    void officialHighlightEqualsUsesPersistedId() {
        var left = new OfficialHighlight();
        setEntityId(left, 7L);
        var right = new OfficialHighlight();
        setEntityId(right, 7L);
        assertThat(left).isEqualTo(right);
        assertThat(left.equals(null)).isFalse();
    }

    @Test
    void postTextHighlightEqualsRejectsNullOrDifferentId() {
        var highlight = new PostTextHighlight();
        setEntityId(highlight, 2L);
        assertThat(highlight.equals(null)).isFalse();
        assertThat(highlight).isNotEqualTo(new PostTextHighlight());
        assertThat(highlight).isEqualTo(highlight);
    }

    @Test
    void postTextHighlightEqualsUsesPersistedId() {
        var left = new PostTextHighlight();
        setEntityId(left, 1L);
        var right = new PostTextHighlight();
        setEntityId(right, 1L);
        assertThat(left).isEqualTo(right);
        assertThat(left).hasSameHashCodeAs(right);
    }
}
