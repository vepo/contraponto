package dev.vepo.contraponto.components.forms;

import jakarta.ws.rs.FormParam;

public record AccountSecurityUpdateRequest(@FormParam("email") String email,
                                           @FormParam("currentPassword") String currentPassword,
                                           @FormParam("newPassword") String newPassword,
                                           @FormParam("confirmPassword") String confirmPassword) {}
