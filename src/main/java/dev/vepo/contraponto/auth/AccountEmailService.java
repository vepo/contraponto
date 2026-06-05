package dev.vepo.contraponto.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.shared.infra.SiteBranding;
import dev.vepo.contraponto.user.User;
import io.quarkus.mailer.MailTemplate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(AccountEmailService.class);

    private final String mailFrom;
    private final String baseUrl;
    private final int passwordResetHours;
    private final int emailChangeHours;
    private final int accountActivationHours;
    private final SiteBranding siteBranding;
    private final AccountEmailMessages emailMessages;
    private final AdminNotifyEmailResolver adminNotifyEmailResolver;
    private final AccountEmailOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Inject
    public AccountEmailService(@ConfigProperty(name = "quarkus.mailer.from", defaultValue = "noreply@contraponto.blog") String mailFrom,
                               @ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String baseUrl,
                               @ConfigProperty(name = "app.account-token.password-reset-hours", defaultValue = "1") int passwordResetHours,
                               @ConfigProperty(name = "app.account-token.email-change-hours", defaultValue = "24") int emailChangeHours,
                               @ConfigProperty(name = "app.account-token.account-activation-hours", defaultValue = "48") int accountActivationHours,
                               SiteBranding siteBranding,
                               AccountEmailMessages emailMessages,
                               AdminNotifyEmailResolver adminNotifyEmailResolver,
                               AccountEmailOutboxRepository outboxRepository,
                               ObjectMapper objectMapper) {
        this.mailFrom = mailFrom;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.passwordResetHours = passwordResetHours;
        this.emailChangeHours = emailChangeHours;
        this.accountActivationHours = accountActivationHours;
        this.siteBranding = siteBranding;
        this.emailMessages = emailMessages;
        this.adminNotifyEmailResolver = adminNotifyEmailResolver;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    private MailTemplate.MailTemplateInstance buildTemplate(AccountEmailOutbox entry) throws JsonProcessingException {
        return switch (entry.getKind()) {
            case ACCOUNT_ACTIVATION -> {
                var payload = objectMapper.readValue(entry.getPayload(), AccountActivationOutboxPayload.class);
                yield new accountActivation(payload.baseUrl(), payload.localeLang(), payload.event(), payload.copy());
            }
            case EMAIL_CHANGED_NOTICE -> {
                var payload = objectMapper.readValue(entry.getPayload(), EmailChangedNoticeOutboxPayload.class);
                yield new emailChangedNotice(payload.baseUrl(), payload.localeLang(), payload.event(), payload.copy());
            }
            case EMAIL_CHANGE_VERIFICATION -> {
                var payload = objectMapper.readValue(entry.getPayload(), EmailChangeVerificationOutboxPayload.class);
                yield new emailChangeVerification(payload.baseUrl(), payload.localeLang(), payload.event(), payload.copy());
            }
            case PASSWORD_CHANGED -> {
                var payload = objectMapper.readValue(entry.getPayload(), PasswordChangedOutboxPayload.class);
                yield new passwordChanged(payload.baseUrl(), payload.localeLang(), payload.event(), payload.copy());
            }
            case PASSWORD_RESET -> {
                var payload = objectMapper.readValue(entry.getPayload(), PasswordResetOutboxPayload.class);
                yield new passwordReset(payload.baseUrl(), payload.localeLang(), payload.event(), payload.copy());
            }
            case UNAUTHORIZED_SIGNUP_REPORT -> {
                var payload = objectMapper.readValue(entry.getPayload(), UnauthorizedSignupReportOutboxPayload.class);
                yield new unauthorizedSignupReport(payload.baseUrl(), payload.event());
            }
        };
    }

    private void enqueueFailure(AccountEmailKind kind, String recipient, String subject, Object payload, Throwable failure) {
        try {
            outboxRepository.persist(AccountEmailOutbox.pending(kind,
                                                                recipient,
                                                                subject,
                                                                objectMapper.writeValueAsString(payload),
                                                                failure.getMessage()));
            LOG.info("Queued account email for later delivery to {} ({})", recipient, kind);
        } catch (JsonProcessingException enqueueFailure) {
            LOG.error("Failed to serialize account email outbox payload for {} ({})", recipient, kind, enqueueFailure);
        } catch (RuntimeException enqueueFailure) {
            LOG.error("Failed to persist account email outbox entry for {} ({})", recipient, kind, enqueueFailure);
        }
    }

    public void resendOutboxEntry(AccountEmailOutbox entry) {
        try {
            sendAndAwait(buildTemplate(entry), entry.getRecipient(), entry.getSubject());
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Invalid account email outbox payload for id=%s".formatted(entry.getId()), failure);
        }
    }

    private void send(AccountEmailKind kind,
                      Object outboxPayload,
                      MailTemplate.MailTemplateInstance template,
                      String recipient,
                      String subject) {
        template.to(recipient)
                .from(mailFrom)
                .subject(subject)
                .send()
                .subscribe()
                .with(ignored -> {},
                      failure -> {
                          LOG.error("Failed to send account email to {} with subject {}", recipient, subject, failure);
                          enqueueFailure(kind, recipient, subject, outboxPayload, failure);
                      });
    }

    public void sendAccountActivation(User user, String rawToken) {
        String activateUrl = "%s/account/activate?token=%s".formatted(baseUrl, rawToken);
        String reportUrl = "%s/account/report-signup?token=%s".formatted(baseUrl, rawToken);
        var event = new AccountActivationEvent(activateUrl,
                                               accountActivationHours,
                                               siteBranding.displayName(),
                                               siteBranding.seoName());
        var copy = AccountEmailCopy.activation(event.siteName(), activateUrl, reportUrl, accountActivationHours, emailMessages);
        var outboxPayload = new AccountActivationOutboxPayload(baseUrl, emailMessages.localeLang(), event, copy);
        send(AccountEmailKind.ACCOUNT_ACTIVATION,
             outboxPayload,
             new accountActivation(baseUrl, emailMessages.localeLang(), event, copy),
             user.getEmail(),
             copy.subject());
    }

    private void sendAndAwait(MailTemplate.MailTemplateInstance template, String recipient, String subject) {
        template.to(recipient).from(mailFrom).subject(subject).sendAndAwait();
    }

    public void sendEmailChangedNotice(String previousEmail, String newEmail) {
        var event = new EmailChangedNoticeEvent(newEmail, siteBranding.displayName(), siteBranding.seoName());
        var copy = AccountEmailCopy.emailChangedNotice(event.siteName(), newEmail, emailMessages);
        var outboxPayload = new EmailChangedNoticeOutboxPayload(baseUrl, emailMessages.localeLang(), event, copy);
        send(AccountEmailKind.EMAIL_CHANGED_NOTICE,
             outboxPayload,
             new emailChangedNotice(baseUrl, emailMessages.localeLang(), event, copy),
             previousEmail,
             copy.subject());
    }

    public void sendEmailChangeVerification(User user, String pendingEmail, String rawToken) {
        String verifyUrl = "%s/account/verify-email?token=%s".formatted(baseUrl, rawToken);
        var event = new EmailChangeVerificationEvent(verifyUrl,
                                                     pendingEmail,
                                                     emailChangeHours,
                                                     siteBranding.displayName(),
                                                     siteBranding.seoName());
        var copy = AccountEmailCopy.emailChangeVerification(event.siteName(), pendingEmail, verifyUrl, emailChangeHours, emailMessages);
        var outboxPayload = new EmailChangeVerificationOutboxPayload(baseUrl, emailMessages.localeLang(), event, copy);
        send(AccountEmailKind.EMAIL_CHANGE_VERIFICATION,
             outboxPayload,
             new emailChangeVerification(baseUrl, emailMessages.localeLang(), event, copy),
             pendingEmail,
             copy.subject());
    }

    public void sendPasswordChanged(User user) {
        var event = new PasswordChangedEvent(siteBranding.displayName(), siteBranding.seoName());
        var copy = AccountEmailCopy.passwordChanged(event.siteName(), emailMessages);
        var outboxPayload = new PasswordChangedOutboxPayload(baseUrl, emailMessages.localeLang(), event, copy);
        send(AccountEmailKind.PASSWORD_CHANGED,
             outboxPayload,
             new passwordChanged(baseUrl, emailMessages.localeLang(), event, copy),
             user.getEmail(),
             copy.subject());
    }

    public void sendPasswordReset(User user, String rawToken) {
        String resetUrl = "%s/password-recovery/reset?token=%s".formatted(baseUrl, rawToken);
        var event = new PasswordResetEvent(resetUrl,
                                           passwordResetHours,
                                           siteBranding.displayName(),
                                           siteBranding.seoName());
        var copy = AccountEmailCopy.passwordReset(event.siteName(), resetUrl, passwordResetHours, emailMessages);
        var outboxPayload = new PasswordResetOutboxPayload(baseUrl, emailMessages.localeLang(), event, copy);
        send(AccountEmailKind.PASSWORD_RESET,
             outboxPayload,
             new passwordReset(baseUrl, emailMessages.localeLang(), event, copy),
             user.getEmail(),
             copy.subject());
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
        var outboxPayload = new UnauthorizedSignupReportOutboxPayload(baseUrl, event);
        for (String recipient : recipients) {
            send(AccountEmailKind.UNAUTHORIZED_SIGNUP_REPORT,
                 outboxPayload,
                 new unauthorizedSignupReport(baseUrl, event),
                 recipient,
                 subject);
        }
    }
}
