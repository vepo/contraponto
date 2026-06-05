package dev.vepo.contraponto.auth;

public record PasswordResetEvent(String resetUrl, int expiresHours, String siteName, String siteSeoName) {}
