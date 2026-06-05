package dev.vepo.contraponto.auth;

public record AccountActivationEvent(String activateUrl, int expiresHours, String siteName, String siteSeoName) {}
