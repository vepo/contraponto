package dev.vepo.contraponto.shared.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Unremovable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Resolves i18n keys for server-driven responses (toast headers, etc.) from the
 * request locale and bundled {@code messages_*.json} files.
 */
@ApplicationScoped
@Unremovable
public class I18nMessageCatalog {

    private static final Map<String, String> PT_BR_DEFAULTS = Map.ofEntries(
                                                                            Map.entry(I18nKeys.TOAST_BLOG_SAVED, I18nDefaults.BLOG_SAVED),
                                                                            Map.entry(I18nKeys.TOAST_BLOG_NOT_FOUND, I18nDefaults.BLOG_NOT_FOUND),
                                                                            Map.entry(I18nKeys.TOAST_BLOG_FORBIDDEN, I18nDefaults.BLOG_FORBIDDEN),
                                                                            Map.entry(I18nKeys.TOAST_POST_PUBLISHED, I18nDefaults.POST_PUBLISHED),
                                                                            Map.entry(I18nKeys.TOAST_DRAFT_SAVED, I18nDefaults.DRAFT_SAVED),
                                                                            Map.entry(I18nKeys.TOAST_POST_CONTENT_REQUIRED, I18nDefaults.POST_CONTENT_REQUIRED),
                                                                            Map.entry(I18nKeys.TOAST_POST_TITLE_REQUIRED, I18nDefaults.POST_TITLE_REQUIRED),
                                                                            Map.entry(I18nKeys.TOAST_POST_INVALID_SLUG, I18nDefaults.POST_INVALID_SLUG),
                                                                            Map.entry(I18nKeys.TOAST_POST_SLUG_EXISTS, I18nDefaults.POST_SLUG_EXISTS),
                                                                            Map.entry(I18nKeys.TOAST_POST_BLOG_REQUIRED, I18nDefaults.POST_BLOG_REQUIRED),
                                                                            Map.entry(I18nKeys.TOAST_PAGE_SAVED, I18nDefaults.PAGE_SAVED),
                                                                            Map.entry(I18nKeys.TOAST_PAGE_DELETED, I18nDefaults.PAGE_DELETED),
                                                                            Map.entry(I18nKeys.TOAST_PAGE_NOT_FOUND, I18nDefaults.PAGE_NOT_FOUND),
                                                                            Map.entry(I18nKeys.TOAST_PAGE_FORBIDDEN, I18nDefaults.PAGE_FORBIDDEN),
                                                                            Map.entry(I18nKeys.TOAST_PAGE_SLUG_EXISTS, I18nDefaults.PAGE_SLUG_EXISTS),
                                                                            Map.entry(I18nKeys.TOAST_PAGE_BLOG_REQUIRED, I18nDefaults.PAGE_BLOG_REQUIRED),
                                                                            Map.entry(I18nKeys.TOAST_USER_CREATED, I18nDefaults.USER_CREATED),
                                                                            Map.entry(I18nKeys.TOAST_USER_SAVED, I18nDefaults.USER_SAVED),
                                                                            Map.entry(I18nKeys.TOAST_USER_NOT_FOUND, I18nDefaults.USER_NOT_FOUND),
                                                                            Map.entry(I18nKeys.TOAST_USER_FORBIDDEN, I18nDefaults.USER_FORBIDDEN),
                                                                            Map.entry(I18nKeys.TOAST_EDITOR_FORBIDDEN, I18nDefaults.EDITOR_FORBIDDEN),
                                                                            Map.entry(I18nKeys.TOAST_ADMIN_FORBIDDEN, I18nDefaults.ADMIN_FORBIDDEN),
                                                                            Map.entry(I18nKeys.TOAST_POST_NOT_FOUND, I18nDefaults.POST_NOT_FOUND),
                                                                            Map.entry(I18nKeys.TOAST_CANNOT_FEATURE_DRAFT, I18nDefaults.CANNOT_FEATURE_DRAFT),
                                                                            Map.entry(I18nKeys.TOAST_COMMENT_NOT_FOUND, I18nDefaults.COMMENT_NOT_FOUND),
                                                                            Map.entry(I18nKeys.TOAST_TAG_UPDATED, I18nDefaults.TAG_UPDATED),
                                                                            Map.entry(I18nKeys.TOAST_TAG_NAME_REQUIRED, I18nDefaults.TAG_NAME_REQUIRED),
                                                                            Map.entry(I18nKeys.TOAST_TAG_SLUG_INVALID, I18nDefaults.TAG_SLUG_INVALID),
                                                                            Map.entry(I18nKeys.TOAST_TAG_SLUG_TAKEN, I18nDefaults.TAG_SLUG_TAKEN),
                                                                            Map.entry(I18nKeys.TOAST_TAG_MISSING, I18nDefaults.TAG_MISSING),
                                                                            Map.entry(I18nKeys.TOAST_IMAGE_UPDATED, I18nDefaults.IMAGE_UPDATED),
                                                                            Map.entry(I18nKeys.TOAST_BLOG_NOT_FOUND_AUDIENCE,
                                                                                      I18nDefaults.BLOG_NOT_FOUND_AUDIENCE),
                                                                            Map.entry(I18nKeys.AUTH_ERROR_LOGIN_REQUIRED,
                                                                                      "Informe seu e-mail ou nome de usuário."),
                                                                            Map.entry(I18nKeys.AUTH_ERROR_INVALID_CREDENTIALS,
                                                                                      "Usuário/e-mail ou senha inválidos."));

    private static final ObjectMapper JSON = new ObjectMapper();

    private static Map<String, String> loadBundle(String resourcePath) throws IOException {
        try (InputStream in = I18nMessageCatalog.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return Map.of();
            }
            var body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JSON.readValue(body, new TypeReference<>() {});
        }
    }

    private final CurrentLocale currentLocale;
    private final LocalePreference localePreference;

    private Map<String, Map<String, String>> translations = Map.of();

    @Inject
    public I18nMessageCatalog(CurrentLocale currentLocale, LocalePreference localePreference) {
        this.currentLocale = currentLocale;
        this.localePreference = localePreference;
    }

    @PostConstruct
    void loadBundles() throws IOException {
        var loaded = new HashMap<String, Map<String, String>>();
        loaded.put("en", loadBundle("/i18n/messages_en.json"));
        loaded.put("es", loadBundle("/i18n/messages_es.json"));
        translations = Collections.unmodifiableMap(loaded);
    }

    public String resolve(String key, String ptBrDefault) {
        if (key == null || key.isBlank()) {
            return ptBrDefault != null ? ptBrDefault : "";
        }
        var ptBr = ptBrDefault != null ? ptBrDefault : PT_BR_DEFAULTS.get(key);
        var locale = localePreference.normalize(currentLocale.get());
        if (LocalePreference.DEFAULT_LOCALE.equals(locale)) {
            return ptBr != null ? ptBr : key;
        }
        var bundle = translations.get(locale);
        if (bundle != null) {
            var translated = bundle.get(key);
            if (translated != null) {
                return translated;
            }
        }
        return ptBr != null ? ptBr : key;
    }
}
