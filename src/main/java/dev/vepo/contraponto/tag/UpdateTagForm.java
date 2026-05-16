package dev.vepo.contraponto.tag;

import jakarta.ws.rs.FormParam;

public record UpdateTagForm(@FormParam("tagId") Long tagId,
                            @FormParam("name") String name,
                            @FormParam("slug") String slug,
                            @FormParam("description") String description) {}
