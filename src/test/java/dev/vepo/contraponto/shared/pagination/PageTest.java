package dev.vepo.contraponto.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class PageTest {
    @Test
    void lastPageTest() {
        var page = new Page<>(IntStream.range(0, 20)
                                       .mapToObj(i -> i)
                                       .toList(),
                              1, // page
                              20, // limit
                              21);
        assertThat(page.isFirstPage()).isTrue();
        assertThat(page.isLastPage()).isFalse();

        page = new Page<>(IntStream.range(0, 20)
                                   .mapToObj(i -> i)
                                   .toList(),
                          2, // page
                          20, // limit
                          40);
        assertThat(page.isFirstPage()).isFalse();
        assertThat(page.isLastPage()).isTrue();

        page = new Page<>(IntStream.range(0, 8)
                                   .mapToObj(i -> i)
                                   .toList(),
                          1, // page
                          20, // limit
                          8);
        assertThat(page.isFirstPage()).isTrue();
        assertThat(page.isLastPage()).isTrue();

        page = new Page<>(IntStream.range(0, 4)
                                   .mapToObj(i -> i)
                                   .toList(),
                          2, // page
                          20, // limit
                          24);
        assertThat(page.isFirstPage()).isFalse();
        assertThat(page.isLastPage()).isTrue();
    }
}
