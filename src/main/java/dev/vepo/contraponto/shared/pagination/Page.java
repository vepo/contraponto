package dev.vepo.contraponto.shared.pagination;

import java.util.List;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record Page<T>(List<T> data, int page, int limit, long total) {
    public boolean requiresPagination() {
        return total > limit;
    }

    public boolean isFirstPage() {
        return page * limit < total;
    }

    public boolean isEmpty() {
        return data.isEmpty();
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

    public static int offset() {
        return 0;
    }
}
