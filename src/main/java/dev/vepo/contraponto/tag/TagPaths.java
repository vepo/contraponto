package dev.vepo.contraponto.tag;

public final class TagPaths {

    public static String url(Tag tag) {
        return "/tags/%s".formatted(tag.getSlug());
    }

    private TagPaths() {}
}
