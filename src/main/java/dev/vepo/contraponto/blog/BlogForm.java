package dev.vepo.contraponto.blog;

import jakarta.ws.rs.FormParam;

public class BlogForm {

    @FormParam("id")
    private Long id;

    @FormParam("name")
    private String name;

    @FormParam("slug")
    private String slug;

    @FormParam("description")
    private String description;

    @FormParam("active")
    private String active;

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public boolean isActive() {
        return active != null;
    }
}
