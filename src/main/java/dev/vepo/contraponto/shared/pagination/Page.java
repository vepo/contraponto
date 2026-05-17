package dev.vepo.contraponto.shared.pagination;

import java.util.List;
import java.util.function.Function;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Page<T>(List<T> data, int page, int limit, long total) {
    public boolean isFirstPage() {
        return page == 1;
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean hasPrevious() {
        return !isFirstPage();
    }

    public boolean hasNext() {
        return !isLastPage();
    }

    public int previousPage() {
        return page - 1;
    }

    public boolean isLastPage() {
        return page * limit >= total;
    }

    public int nextPage() {
        return page + 1;
    }

    public int totalPages() {
        if (total == 0) {
            return 0;
        }
        return (int) ((total + limit - 1) / limit);
    }

    public int rangeStart() {
        if (total == 0) {
            return 0;
        }
        return (page - 1) * limit + 1;
    }

    public int rangeEnd() {
        if (total == 0) {
            return 0;
        }
        return Math.min(page * limit, (int) total);
    }

    public <U> Page<U> map(Function<T, U> mapper) {
        return new Page<>(data.stream().map(mapper).toList(), page, limit, total);
    }
}
