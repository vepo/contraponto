package dev.vepo.contraponto.shared.pagination;

public record PageQuery(int maxResults, int skip, int limit, int page) {
    public static PageQuery forFeaturedGrid(int limit, int page) {
        var extraFirst = (page == 1) ? 1 : 0;
        var effectiveLimit = limit + extraFirst;
        var offset = (page == 1) ? 0 : ((page - 1) * limit) + 1; // skip the extra from first page
        return new PageQuery(effectiveLimit, offset, limit, page);
    }

    public static PageQuery forGrid(int limit, int page) {
        return new PageQuery(limit, (page - 1) * limit, limit, page);
    }
}
