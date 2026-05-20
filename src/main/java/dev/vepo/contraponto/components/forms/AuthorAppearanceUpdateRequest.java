package dev.vepo.contraponto.components.forms;

import jakarta.ws.rs.FormParam;

public record AuthorAppearanceUpdateRequest(@FormParam("name") String name,
                                            @FormParam("currentPassword") String currentPassword,
                                            @FormParam("profilePictureId") String profilePictureId,
                                            @FormParam("defaultBannerId") String defaultBannerId,
                                            @FormParam("profileDescription") String profileDescription,
                                            @FormParam("websiteUrl") String websiteUrl,
                                            @FormParam("twitterUrl") String twitterUrl,
                                            @FormParam("mastodonUrl") String mastodonUrl,
                                            @FormParam("githubUrl") String githubUrl,
                                            @FormParam("linkedinUrl") String linkedinUrl) {}
