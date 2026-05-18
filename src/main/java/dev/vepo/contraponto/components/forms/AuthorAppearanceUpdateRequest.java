package dev.vepo.contraponto.components.forms;

import jakarta.ws.rs.FormParam;

public record AuthorAppearanceUpdateRequest(@FormParam("name") String name,
                                            @FormParam("currentPassword") String currentPassword,
                                            @FormParam("profilePictureId") String profilePictureId,
                                            @FormParam("defaultBannerId") String defaultBannerId) {}
