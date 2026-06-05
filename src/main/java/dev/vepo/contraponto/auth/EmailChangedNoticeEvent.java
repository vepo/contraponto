package dev.vepo.contraponto.auth;

public record EmailChangedNoticeEvent(String newEmail, String siteName, String siteSeoName) {}
