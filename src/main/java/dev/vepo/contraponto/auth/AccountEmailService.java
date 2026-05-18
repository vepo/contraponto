package dev.vepo.contraponto.auth;

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

    @Inject
    public AccountEmailService(Mailer mailer,
                               @Location("auth/password-reset-email") Template passwordResetEmail,
                               @Location("auth/password-changed-email") Template passwordChangedEmail,
                               @Location("auth/email-change-verification-email") Template emailChangeVerificationEmail,
                               @Location("auth/email-changed-notice-email") Template emailChangedNoticeEmail,
                               @ConfigProperty(name = "quarkus.mailer.from", defaultValue = "noreply@contraponto.blog") String mailFrom,
                               @ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String baseUrl,
                               @ConfigProperty(name = "app.account-token.password-reset-hours", defaultValue = "1") int passwordResetHours,
                               @ConfigProperty(name = "app.account-token.email-change-hours", defaultValue = "24") int emailChangeHours) {
        this.mailer = mailer;
        this.passwordResetEmail = passwordResetEmail;
        this.passwordChangedEmail = passwordChangedEmail;
        this.emailChangeVerificationEmail = emailChangeVerificationEmail;
        this.emailChangedNoticeEmail = emailChangedNoticeEmail;
        this.mailFrom = mailFrom;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.passwordResetHours = passwordResetHours;
        this.emailChangeHours = emailChangeHours;
    }

    public void sendEmailChangeVerification(User user, String pendingEmail, String rawToken) {
        String verifyUrl = baseUrl + "/account/verify-email?token=" + rawToken;
        String html = emailChangeVerificationEmail.data("verifyUrl", verifyUrl)
                                                  .data("newEmail", pendingEmail)
                                                  .data("baseUrl", baseUrl)
                                                  .data("expiresHours", emailChangeHours)
                                                  .render();
        mailer.send(Mail.withHtml(pendingEmail, "Confirm your new email address", html).setFrom(mailFrom));
    }

    public void sendEmailChangedNotice(String previousEmail, String newEmail) {
        String html = emailChangedNoticeEmail.data("newEmail", newEmail)
                                             .data("baseUrl", baseUrl)
                                             .render();
        mailer.send(Mail.withHtml(previousEmail, "Your contraponto email address was changed", html).setFrom(mailFrom));
    }

    public void sendPasswordChanged(User user) {
        String html = passwordChangedEmail.data("baseUrl", baseUrl).render();
        mailer.send(Mail.withHtml(user.getEmail(), "Your contraponto password was changed", html).setFrom(mailFrom));
    }

    public void sendPasswordReset(User user, String rawToken) {
        String resetUrl = baseUrl + "/password-recovery/reset?token=" + rawToken;
        String html = passwordResetEmail.data("resetUrl", resetUrl)
                                        .data("baseUrl", baseUrl)
                                        .data("expiresHours", passwordResetHours)
                                        .render();
        mailer.send(Mail.withHtml(user.getEmail(), "Reset your contraponto password", html).setFrom(mailFrom));
    }
}
