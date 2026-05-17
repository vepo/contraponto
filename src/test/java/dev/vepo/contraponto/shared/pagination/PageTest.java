package dev.vepo.contraponto.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

    @Test
    void paginationHelpers() {
        var page = new Page<>(IntStream.range(0, 20).mapToObj(i -> i).toList(), 2, 20, 45);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.rangeStart()).isEqualTo(21);
        assertThat(page.rangeEnd()).isEqualTo(40);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();

        var empty = new Page<>(List.<Integer>of(), 1, 20, 0);
        assertThat(empty.totalPages()).isZero();
        assertThat(empty.rangeStart()).isZero();
        assertThat(empty.rangeEnd()).isZero();
    }
}
