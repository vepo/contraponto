package dev.vepo.contraponto.postresponse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.UnitTest;

@UnitTest
class PostResponseEqualityTest {

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
    void equalsRejectsNullOrDifferentId() {
        var response = new PostResponse();
        setEntityId(response, 1L);
        assertThat(response.equals(null)).isFalse();
        assertThat(response).isNotEqualTo(new PostResponse());
        assertThat(response).isEqualTo(response);
    }

    @Test
    void equalsUsesPersistedId() {
        var left = new PostResponse();
        setEntityId(left, 4L);
        var right = new PostResponse();
        setEntityId(right, 4L);
        assertThat(left).isEqualTo(right);
        assertThat(left).hasSameHashCodeAs(right);
    }
}
