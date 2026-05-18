package dev.vepo.contraponto.blog;

import dev.vepo.contraponto.shared.infra.LoggedUser;

public record BlogRow(Long id,
                      String name,
                      String slug,
                      String owner,
                      String publicUrl,
                      boolean main,
                      boolean active,
                      String description,
                      boolean canEdit,
                      boolean canDeactivate) {

    public static BlogRow from(Blog blog, BlogAccess blogAccess, LoggedUser viewer) {
        var owner = blog.getOwner();
        return new BlogRow(blog.getId(),
                           blog.getName(),
                           blog.getSlug(),
                           owner.getUsername(),
                           BlogEndpoint.extractUrl(blog),
                           blog.isMain(),
                           blog.isActive(),
                           blog.getDescription(),
                           blogAccess.canEdit(blog, viewer),
                           blogAccess.canDeactivate(blog, viewer));
    }
}
