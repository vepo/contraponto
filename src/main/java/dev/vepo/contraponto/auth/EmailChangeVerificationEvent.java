package dev.vepo.contraponto.auth;

public record EmailChangeVerificationEvent(String verifyUrl,
                                           String newEmail,
                                           int expiresHours,
                                           String siteName,
                                           String siteSeoName) {}
