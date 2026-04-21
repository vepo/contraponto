package dev.vepo.contraponto.components.forms;

import jakarta.ws.rs.FormParam;

public record ProfileUpdateRequest(@FormParam("name") String name,
                                   @FormParam("email") String email,
                                   @FormParam("currentPassword") String currentPassword,
                                   @FormParam("newPassword") String newPassword,
                                   @FormParam("confirmPassword") String confirmPassword) {}
