package dev.vepo.contraponto.custompage;

import jakarta.ws.rs.FormParam;

public class CustomPageForm {

    @FormParam("id")
    private Long id;

    @FormParam("title")
    private String title;

    @FormParam("slug")
    private String slug;

    @FormParam("section")
    private String section;

    @FormParam("content")
    private String content;

    @FormParam("placement")
    private String placement;

    @FormParam("published")
    private String published;

    @FormParam("scope")
    private String scope;

    @FormParam("blogId")
    private Long blogId;

    public Long getBlogId() {
        return blogId;
    }

    public String getContent() {
        return content;
    }

    public Long getId() {
        return id;
    }

    public PagePlacement getPlacement() {
        if (placement == null || placement.isBlank()) {
            return PagePlacement.NONE;
        }
        return PagePlacement.valueOf(placement);
    }

    public String getScope() {
        return scope;
    }

    public String getSection() {
        return section;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public boolean isApplicationScope() {
        return "application".equalsIgnoreCase(scope);
    }

    public boolean isPublished() {
        return published != null;
    }
}
