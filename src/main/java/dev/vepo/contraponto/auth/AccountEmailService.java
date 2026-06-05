package dev.vepo.contraponto.auth;

import dev.vepo.contraponto.shared.infra.SiteBranding;
import dev.vepo.contraponto.user.User;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AccountEmailService {

    private final Mailer mailer;
    private final Template passwordResetEmail;
    private final Template passwordChangedEmail;
    private final Template emailChangeVerificationEmail;
    private final Template emailChangedNoticeEmail;
    private final String mailFrom;
    private final String baseUrl;
    private final int passwordResetHours;
    private final int emailChangeHours;
    private final SiteBranding siteBranding;

    @Inject
    public AccountEmailService(Mailer mailer,
                               @Location("auth/password-reset-email") Template passwordResetEmail,
                               @Location("auth/password-changed-email") Template passwordChangedEmail,
                               @Location("auth/email-change-verification-email") Template emailChangeVerificationEmail,
                               @Location("auth/email-changed-notice-email") Template emailChangedNoticeEmail,
                               @ConfigProperty(name = "quarkus.mailer.from", defaultValue = "noreply@contraponto.blog") String mailFrom,
                               @ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String baseUrl,
                               @ConfigProperty(name = "app.account-token.password-reset-hours", defaultValue = "1") int passwordResetHours,
                               @ConfigProperty(name = "app.account-token.email-change-hours", defaultValue = "24") int emailChangeHours,
                               SiteBranding siteBranding) {
        this.mailer = mailer;
        this.passwordResetEmail = passwordResetEmail;
        this.passwordChangedEmail = passwordChangedEmail;
        this.emailChangeVerificationEmail = emailChangeVerificationEmail;
        this.emailChangedNoticeEmail = emailChangedNoticeEmail;
        this.mailFrom = mailFrom;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.passwordResetHours = passwordResetHours;
        this.emailChangeHours = emailChangeHours;
        this.siteBranding = siteBranding;
    }

    public void sendEmailChangedNotice(String previousEmail, String newEmail) {
        String html = emailChangedNoticeEmail.data("newEmail", newEmail)
                                             .data("baseUrl", baseUrl)
                                             .data("siteName", siteBranding.displayName())
                                             .data("siteSeoName", siteBranding.seoName())
                                             .render();
        mailer.send(Mail.withHtml(previousEmail,
                                  "Your %s email address was changed".formatted(siteBranding.displayName()),
                                  html)
                        .setFrom(mailFrom));
    }

    public void sendEmailChangeVerification(User user, String pendingEmail, String rawToken) {
        String verifyUrl = "%s/account/verify-email?token=%s".formatted(baseUrl, rawToken);
        String html = emailChangeVerificationEmail.data("verifyUrl", verifyUrl)
                                                  .data("newEmail", pendingEmail)
                                                  .data("baseUrl", baseUrl)
                                                  .data("expiresHours", emailChangeHours)
                                                  .data("siteName", siteBranding.displayName())
                                                  .data("siteSeoName", siteBranding.seoName())
                                                  .render();
        mailer.send(Mail.withHtml(pendingEmail, "Confirm your new email address", html).setFrom(mailFrom));
    }

    public void sendPasswordChanged(User user) {
        String html = passwordChangedEmail.data("baseUrl", baseUrl)
                                          .data("siteName", siteBranding.displayName())
                                          .data("siteSeoName", siteBranding.seoName())
                                          .render();
        mailer.send(Mail.withHtml(user.getEmail(),
                                  "Your %s password was changed".formatted(siteBranding.displayName()),
                                  html)
                        .setFrom(mailFrom));
    }

    public void sendPasswordReset(User user, String rawToken) {
        String resetUrl = "%s/password-recovery/reset?token=%s".formatted(baseUrl, rawToken);
        String html = passwordResetEmail.data("resetUrl", resetUrl)
                                        .data("baseUrl", baseUrl)
                                        .data("expiresHours", passwordResetHours)
                                        .data("siteName", siteBranding.displayName())
                                        .data("siteSeoName", siteBranding.seoName())
                                        .render();
        mailer.send(Mail.withHtml(user.getEmail(),
                                  "Reset your %s password".formatted(siteBranding.displayName()),
                                  html)
                        .setFrom(mailFrom));
    }
}
