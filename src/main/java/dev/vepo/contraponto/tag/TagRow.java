package dev.vepo.contraponto.tag;

public record TagRow(Long id, String name, String slug, String description, String publicUrl) {

    public static TagRow from(Tag tag) {
        String description = tag.getDescription();
        if (description != null && description.isBlank()) {
            description = null;
        }
        return new TagRow(tag.getId(),
                          tag.getName(),
                          tag.getSlug(),
                          description,
                          TagPaths.url(tag));
    }
}
