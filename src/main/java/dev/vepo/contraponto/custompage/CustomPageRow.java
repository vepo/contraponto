package dev.vepo.contraponto.custompage;

public record CustomPageRow(Long id,
                            String title,
                            String scope,
                            String publicUrl,
                            String section,
                            PagePlacement placement,
                            boolean published) {

    public static CustomPageRow from(CustomPage page) {
        return new CustomPageRow(page.getId(),
                                 page.getTitle(),
                                 scopeLabel(page),
                                 CustomPagePaths.publicUrl(page),
                                 page.getSection(),
                                 page.getPlacement(),
                                 page.isPublished());
    }

    private static String scopeLabel(CustomPage page) {
        var blog = page.getBlog();
        if (blog == null) {
            return "Application";
        }
        var owner = blog.getOwner();
        if (blog.isMain()) {
            return "%s (default blog)".formatted(owner.getUsername());
        }
        return "%s / %s".formatted(owner.getUsername(), blog.getSlug());
    }
}
