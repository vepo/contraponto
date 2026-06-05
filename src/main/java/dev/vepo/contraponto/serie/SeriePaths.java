package dev.vepo.contraponto.serie;

public final class SeriePaths {

    public static String extractUrl(Serie serie) {
        var blog = serie.getBlog();
        if (blog.isMain()) {
            return "/%s/serie/%s".formatted(blog.getOwner().getUsername(), serie.getSlug());
        }
        return "/%s/%s/serie/%s".formatted(blog.getOwner().getUsername(), blog.getSlug(), serie.getSlug());
    }

    private SeriePaths() {}
}
