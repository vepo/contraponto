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

    @FormParam("git_enabled")
    private String gitEnabled;

    @FormParam("git_remote_url")
    private String gitRemoteUrl;

    @FormParam("git_branch")
    private String gitBranch;

    public String getDescription() {
        return description;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public String getGitRemoteUrl() {
        return gitRemoteUrl;
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

    public boolean isGitEnabled() {
        return gitEnabled != null;
    }
}
