package dev.vepo.contraponto.blog;

public record BlogRow(Long id,
                      String name,
                      String slug,
                      String owner,
                      String publicUrl,
                      boolean main,
                      boolean active,
                      String description) {

    public static BlogRow from(Blog blog) {
        var owner = blog.getOwner();
        return new BlogRow(blog.getId(),
                           blog.getName(),
                           blog.getSlug(),
                           owner.getUsername(),
                           BlogEndpoint.extractUrl(blog),
                           blog.isMain(),
                           blog.isActive(),
                           blog.getDescription());
    }
}
