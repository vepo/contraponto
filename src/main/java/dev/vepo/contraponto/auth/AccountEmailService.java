package dev.vepo.contraponto.auth;

import dev.vepo.contraponto.shared.infra.SiteBranding;
import dev.vepo.contraponto.user.User;
import io.quarkus.mailer.MailTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AccountEmailService {

    record accountActivation(String baseUrl, String localeLang, AccountActivationEvent event, AccountEmailCopy copy)
            implements MailTemplate.MailTemplateInstance {}

    record emailChangedNotice(String baseUrl, String localeLang, EmailChangedNoticeEvent event, AccountEmailCopy copy)
            implements MailTemplate.MailTemplateInstance {}

    record emailChangeVerification(String baseUrl, String localeLang, EmailChangeVerificationEvent event, AccountEmailCopy copy)
            implements MailTemplate.MailTemplateInstance {}

    record passwordChanged(String baseUrl, String localeLang, PasswordChangedEvent event, AccountEmailCopy copy)
            implements MailTemplate.MailTemplateInstance {}

    record passwordReset(String baseUrl, String localeLang, PasswordResetEvent event, AccountEmailCopy copy)
            implements MailTemplate.MailTemplateInstance {}

    record unauthorizedSignupReport(String baseUrl, UnauthorizedSignupReportEvent event)
            implements MailTemplate.MailTemplateInstance {}

    private final String mailFrom;
    private final String baseUrl;
    private final int passwordResetHours;
    private final int emailChangeHours;
    private final int accountActivationHours;
    private final SiteBranding siteBranding;
    private final AccountEmailMessages emailMessages;
    private final AdminNotifyEmailResolver adminNotifyEmailResolver;

    @Inject
    public AccountEmailService(@ConfigProperty(name = "quarkus.mailer.from", defaultValue = "noreply@contraponto.blog") String mailFrom,
                               @ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String baseUrl,
                               @ConfigProperty(name = "app.account-token.password-reset-hours", defaultValue = "1") int passwordResetHours,
                               @ConfigProperty(name = "app.account-token.email-change-hours", defaultValue = "24") int emailChangeHours,
                               @ConfigProperty(name = "app.account-token.account-activation-hours", defaultValue = "48") int accountActivationHours,
                               SiteBranding siteBranding,
                               AccountEmailMessages emailMessages,
                               AdminNotifyEmailResolver adminNotifyEmailResolver) {
        this.mailFrom = mailFrom;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.passwordResetHours = passwordResetHours;
        this.emailChangeHours = emailChangeHours;
        this.accountActivationHours = accountActivationHours;
        this.siteBranding = siteBranding;
        this.emailMessages = emailMessages;
        this.adminNotifyEmailResolver = adminNotifyEmailResolver;
    }

    private void send(MailTemplate.MailTemplateInstance template, String recipient, String subject) {
        template.to(recipient).from(mailFrom).subject(subject).sendAndAwait();
    }

    public void sendAccountActivation(User user, String rawToken) {
        String activateUrl = "%s/account/activate?token=%s".formatted(baseUrl, rawToken);
        String reportUrl = "%s/account/report-signup?token=%s".formatted(baseUrl, rawToken);
        var event = new AccountActivationEvent(activateUrl,
                                               accountActivationHours,
                                               siteBranding.displayName(),
                                               siteBranding.seoName());
        var copy = AccountEmailCopy.activation(event.siteName(), activateUrl, reportUrl, accountActivationHours, emailMessages);
        send(new accountActivation(baseUrl, emailMessages.localeLang(), event, copy), user.getEmail(), copy.subject());
    }

    public void sendEmailChangedNotice(String previousEmail, String newEmail) {
        var event = new EmailChangedNoticeEvent(newEmail, siteBranding.displayName(), siteBranding.seoName());
        var copy = AccountEmailCopy.emailChangedNotice(event.siteName(), newEmail, emailMessages);
        send(new emailChangedNotice(baseUrl, emailMessages.localeLang(), event, copy), previousEmail, copy.subject());
    }

    public void sendEmailChangeVerification(User user, String pendingEmail, String rawToken) {
        String verifyUrl = "%s/account/verify-email?token=%s".formatted(baseUrl, rawToken);
        var event = new EmailChangeVerificationEvent(verifyUrl,
                                                     pendingEmail,
                                                     emailChangeHours,
                                                     siteBranding.displayName(),
                                                     siteBranding.seoName());
        var copy = AccountEmailCopy.emailChangeVerification(event.siteName(), pendingEmail, verifyUrl, emailChangeHours, emailMessages);
        send(new emailChangeVerification(baseUrl, emailMessages.localeLang(), event, copy), pendingEmail, copy.subject());
    }

    public void sendPasswordChanged(User user) {
        var event = new PasswordChangedEvent(siteBranding.displayName(), siteBranding.seoName());
        var copy = AccountEmailCopy.passwordChanged(event.siteName(), emailMessages);
        send(new passwordChanged(baseUrl, emailMessages.localeLang(), event, copy), user.getEmail(), copy.subject());
    }

    public void sendPasswordReset(User user, String rawToken) {
        String resetUrl = "%s/password-recovery/reset?token=%s".formatted(baseUrl, rawToken);
        var event = new PasswordResetEvent(resetUrl,
                                           passwordResetHours,
                                           siteBranding.displayName(),
                                           siteBranding.seoName());
        var copy = AccountEmailCopy.passwordReset(event.siteName(), resetUrl, passwordResetHours, emailMessages);
        send(new passwordReset(baseUrl, emailMessages.localeLang(), event, copy), user.getEmail(), copy.subject());
    }

    public void sendUnauthorizedSignupReport(User user) {
        var recipients = adminNotifyEmailResolver.resolve();
        if (recipients.isEmpty()) {
            return;
        }
        var event = new UnauthorizedSignupReportEvent(siteBranding.displayName(),
                                                      user.getUsername(),
                                                      user.getEmail(),
                                                      user.getName(),
                                                      "%s/administration/users".formatted(baseUrl));
        String subject = "Unauthorized signup reported on %s".formatted(event.siteName());
        for (String recipient : recipients) {
            new unauthorizedSignupReport(baseUrl, event).to(recipient)
                                                        .from(mailFrom)
                                                        .subject(subject)
                                                        .sendAndAwait();
        }
    }
}
