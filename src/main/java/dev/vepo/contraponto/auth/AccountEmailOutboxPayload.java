package dev.vepo.contraponto.auth;

record AccountActivationOutboxPayload(String baseUrl, String localeLang, AccountActivationEvent event, AccountEmailCopy copy) {}

record EmailChangedNoticeOutboxPayload(String baseUrl, String localeLang, EmailChangedNoticeEvent event, AccountEmailCopy copy) {}

record EmailChangeVerificationOutboxPayload(String baseUrl,
                                            String localeLang,
                                            EmailChangeVerificationEvent event,
                                            AccountEmailCopy copy) {}

record PasswordChangedOutboxPayload(String baseUrl, String localeLang, PasswordChangedEvent event, AccountEmailCopy copy) {}

record PasswordResetOutboxPayload(String baseUrl, String localeLang, PasswordResetEvent event, AccountEmailCopy copy) {}

record UnauthorizedSignupReportOutboxPayload(String baseUrl, UnauthorizedSignupReportEvent event) {}
