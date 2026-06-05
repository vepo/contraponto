package dev.vepo.contraponto.auth;

public record AccountEmailCopy(String subject,
                               String title,
                               String greeting,
                               String body,
                               String actionLabel,
                               String actionUrl,
                               String note,
                               String footerReason,
                               String footerSecurityLabel,
                               String reportUrl,
                               String reportLabel) {

    public static AccountEmailCopy activation(String siteName,
                                              String activateUrl,
                                              String reportUrl,
                                              int expiresHours,
                                              AccountEmailMessages messages) {
        return new AccountEmailCopy(
                                    messages.activationSubject(siteName),
                                    messages.activationTitle(),
                                    messages.greeting(),
                                    messages.activationBody(siteName),
                                    messages.activationAction(),
                                    activateUrl,
                                    messages.activationExpiry(expiresHours),
                                    messages.activationFooterReason(),
                                    messages.footerSecurity(),
                                    reportUrl,
                                    messages.activationReport());
    }

    public static AccountEmailCopy passwordReset(String siteName,
                                                 String resetUrl,
                                                 int expiresHours,
                                                 AccountEmailMessages messages) {
        return new AccountEmailCopy(
                                    messages.passwordResetSubject(siteName),
                                    messages.passwordResetTitle(),
                                    messages.greeting(),
                                    messages.passwordResetBody(siteName),
                                    messages.passwordResetAction(),
                                    resetUrl,
                                    messages.passwordResetExpiry(expiresHours),
                                    messages.passwordResetFooterReason(),
                                    messages.footerSecurity(),
                                    null,
                                    null);
    }

    public static AccountEmailCopy passwordChanged(String siteName, AccountEmailMessages messages) {
        return new AccountEmailCopy(
                                    messages.passwordChangedSubject(siteName),
                                    messages.passwordChangedTitle(),
                                    messages.greeting(),
                                    messages.passwordChangedBody(siteName),
                                    null,
                                    null,
                                    messages.passwordChangedWarning(),
                                    messages.passwordChangedFooterReason(),
                                    messages.footerSecurity(),
                                    null,
                                    null);
    }

    public static AccountEmailCopy emailChangeVerification(String siteName,
                                                           String pendingEmail,
                                                           String verifyUrl,
                                                           int expiresHours,
                                                           AccountEmailMessages messages) {
        return new AccountEmailCopy(
                                    messages.emailChangeVerificationSubject(),
                                    messages.emailChangeVerificationTitle(),
                                    messages.greeting(),
                                    messages.emailChangeVerificationBody(siteName),
                                    messages.emailChangeVerificationAction(pendingEmail),
                                    verifyUrl,
                                    messages.emailChangeVerificationExpiry(expiresHours),
                                    messages.emailChangeVerificationFooterReason(),
                                    messages.footerSecurity(),
                                    null,
                                    null);
    }

    public static AccountEmailCopy emailChangedNotice(String siteName,
                                                      String newEmail,
                                                      AccountEmailMessages messages) {
        return new AccountEmailCopy(
                                    messages.emailChangedNoticeSubject(siteName),
                                    messages.emailChangedNoticeTitle(),
                                    messages.greeting(),
                                    messages.emailChangedNoticeBody(siteName, newEmail),
                                    null,
                                    null,
                                    messages.emailChangedNoticeWarning(),
                                    messages.emailChangedNoticeFooterReason(),
                                    messages.footerSecurity(),
                                    null,
                                    null);
    }
}
