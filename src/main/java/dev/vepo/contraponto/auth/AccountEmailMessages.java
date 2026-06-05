package dev.vepo.contraponto.auth;

import dev.vepo.contraponto.shared.i18n.CurrentLocale;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.i18n.I18nMessageCatalog;
import dev.vepo.contraponto.shared.i18n.LocalePreference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AccountEmailMessages {

    private final I18nMessageCatalog catalog;
    private final CurrentLocale currentLocale;
    private final LocalePreference localePreference;

    @Inject
    public AccountEmailMessages(I18nMessageCatalog catalog,
                                CurrentLocale currentLocale,
                                LocalePreference localePreference) {
        this.catalog = catalog;
        this.currentLocale = currentLocale;
        this.localePreference = localePreference;
    }

    public String activationAction() {
        return msg(I18nKeys.ACCOUNT_EMAIL_ACTIVATION_ACTION, "Ativar conta");
    }

    public String activationBody(String siteName) {
        return format(I18nKeys.ACCOUNT_EMAIL_ACTIVATION_BODY,
                      "Obrigado por se cadastrar no %s. Confirme seu endereço de e-mail para ativar sua conta.",
                      siteName);
    }

    public String activationExpiry(int hours) {
        return format(I18nKeys.ACCOUNT_EMAIL_ACTIVATION_EXPIRY,
                      "Este link expira em %d hora(s). Se você não criou uma conta, use o link abaixo para notificar o administrador do site.",
                      hours);
    }

    public String activationFooterReason() {
        return msg(I18nKeys.ACCOUNT_EMAIL_ACTIVATION_FOOTER,
                   "Você recebeu este e-mail porque uma nova conta foi criada com este endereço.");
    }

    public String activationReport() {
        return msg(I18nKeys.ACCOUNT_EMAIL_ACTIVATION_REPORT, "Notificar administrador");
    }

    public String activationSubject(String siteName) {
        return format(I18nKeys.ACCOUNT_EMAIL_ACTIVATION_SUBJECT, "Ative sua conta no %s", siteName);
    }

    public String activationTitle() {
        return msg(I18nKeys.ACCOUNT_EMAIL_ACTIVATION_TITLE, "Ativar conta");
    }

    public String emailChangedNoticeBody(String siteName, String newEmail) {
        return format(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGED_BODY,
                      "O e-mail da sua conta no %s foi alterado para %s.",
                      siteName,
                      newEmail);
    }

    public String emailChangedNoticeFooterReason() {
        return msg(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGED_FOOTER,
                   "Você recebeu este e-mail porque o endereço de e-mail da sua conta foi alterado.");
    }

    public String emailChangedNoticeSubject(String siteName) {
        return format(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGED_SUBJECT,
                      "Seu endereço de e-mail no %s foi alterado",
                      siteName);
    }

    public String emailChangedNoticeTitle() {
        return msg(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGED_TITLE, "E-mail alterado");
    }

    public String emailChangedNoticeWarning() {
        return msg(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGED_WARNING,
                   "Se você não fez esta alteração, entre em contato com o suporte imediatamente.");
    }

    public String emailChangeVerificationAction(String pendingEmail) {
        return format(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_ACTION, "Confirmar %s", pendingEmail);
    }

    public String emailChangeVerificationBody(String siteName) {
        return format(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_BODY,
                      "Confirme seu novo endereço de e-mail na sua conta do %s:",
                      siteName);
    }

    public String emailChangeVerificationExpiry(int hours) {
        return format(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_EXPIRY,
                      "Este link expira em %d hora(s). Seu e-mail atual permanece ativo até a confirmação.",
                      hours);
    }

    public String emailChangeVerificationFooterReason() {
        return msg(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_FOOTER,
                   "Você recebeu este e-mail porque uma alteração de e-mail foi solicitada na sua conta.");
    }

    public String emailChangeVerificationSubject() {
        return msg(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_SUBJECT, "Confirme seu novo endereço de e-mail");
    }

    public String emailChangeVerificationTitle() {
        return msg(I18nKeys.ACCOUNT_EMAIL_EMAIL_CHANGE_VERIFY_TITLE, "Confirmar e-mail");
    }

    public String footerSecurity() {
        return msg(I18nKeys.ACCOUNT_EMAIL_FOOTER_SECURITY, "Segurança da conta");
    }

    private String format(String key, String ptBrDefault, Object... args) {
        return msg(key, ptBrDefault).formatted(args);
    }

    public String greeting() {
        return msg(I18nKeys.ACCOUNT_EMAIL_GREETING, "Olá,");
    }

    private String locale() {
        return localePreference.normalize(currentLocale.get());
    }

    public String localeLang() {
        return localePreference.toBcp47(locale());
    }

    private String msg(String key, String ptBrDefault) {
        return catalog.resolve(key, ptBrDefault, locale());
    }

    public String passwordChangedBody(String siteName) {
        return format(I18nKeys.ACCOUNT_EMAIL_PASSWORD_CHANGED_BODY,
                      "A senha da sua conta no %s foi alterada.",
                      siteName);
    }

    public String passwordChangedFooterReason() {
        return msg(I18nKeys.ACCOUNT_EMAIL_PASSWORD_CHANGED_FOOTER,
                   "Você recebeu este e-mail porque a senha da sua conta foi alterada.");
    }

    public String passwordChangedSubject(String siteName) {
        return format(I18nKeys.ACCOUNT_EMAIL_PASSWORD_CHANGED_SUBJECT, "Sua senha no %s foi alterada", siteName);
    }

    public String passwordChangedTitle() {
        return msg(I18nKeys.ACCOUNT_EMAIL_PASSWORD_CHANGED_TITLE, "Senha alterada");
    }

    public String passwordChangedWarning() {
        return msg(I18nKeys.ACCOUNT_EMAIL_PASSWORD_CHANGED_WARNING,
                   "Se você não fez esta alteração, entre em contato com o suporte imediatamente e redefina sua senha.");
    }

    public String passwordResetAction() {
        return msg(I18nKeys.ACCOUNT_EMAIL_PASSWORD_RESET_ACTION, "Redefinir senha");
    }

    public String passwordResetBody(String siteName) {
        return format(I18nKeys.ACCOUNT_EMAIL_PASSWORD_RESET_BODY,
                      "Recebemos uma solicitação para redefinir a senha da sua conta no %s.",
                      siteName);
    }

    public String passwordResetExpiry(int hours) {
        return format(I18nKeys.ACCOUNT_EMAIL_PASSWORD_RESET_EXPIRY,
                      "Este link expira em %d hora(s). Se você não solicitou a redefinição, ignore este e-mail.",
                      hours);
    }

    public String passwordResetFooterReason() {
        return msg(I18nKeys.ACCOUNT_EMAIL_PASSWORD_RESET_FOOTER,
                   "Você recebeu este e-mail porque uma redefinição de senha foi solicitada para sua conta.");
    }

    public String passwordResetSubject(String siteName) {
        return format(I18nKeys.ACCOUNT_EMAIL_PASSWORD_RESET_SUBJECT, "Redefina sua senha no %s", siteName);
    }

    public String passwordResetTitle() {
        return msg(I18nKeys.ACCOUNT_EMAIL_PASSWORD_RESET_TITLE, "Redefinir senha");
    }
}
