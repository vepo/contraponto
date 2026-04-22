package dev.vepo.contraponto.components.forms;

import jakarta.ws.rs.FormParam;

public record SaveDraftRequest(@FormParam("postId") Long id,
                               @FormParam("slug") String slug,
                               @FormParam("title") String title,
                               @FormParam("description") String description,
                               @FormParam("content") String content) {}
