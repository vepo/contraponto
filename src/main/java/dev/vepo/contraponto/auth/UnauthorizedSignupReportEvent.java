package dev.vepo.contraponto.auth;

public record UnauthorizedSignupReportEvent(String siteName,
                                            String username,
                                            String email,
                                            String name,
                                            String usersManageUrl) {}
