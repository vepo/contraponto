/**
 * Client-side i18n for interface chrome. Default locale is pt-BR (text in HTML).
 * Secondary locales load JSON from GET /i18n/messages/{locale}.json.
 */
class I18nManager {
    static DEFAULT_LOCALE = 'pt-BR';
    static COOKIE_NAME = 'contraponto_locale';

    constructor() {
        this.locale = I18nManager.DEFAULT_LOCALE;
        this.messages = null;
        this.loadPromise = null;
        this.ready = false;
    }

    init() {
        const meta = document.querySelector('meta[name="app-locale"]');
        this.locale = meta?.content?.trim() || this.readCookie() || I18nManager.DEFAULT_LOCALE;
        this.applyDocumentLang();
        this.loadPromise = this.ensureLoaded().then(() => {
            this.ready = true;
            document.documentElement.dataset.i18nReady = 'true';
            this.apply(document);
            this.syncLocalePickers();
        });
        this.registerHtmxHooks();
        return this.loadPromise;
    }

    readCookie() {
        const match = document.cookie.match(new RegExp('(?:^|; )' + I18nManager.COOKIE_NAME + '=([^;]*)'));
        return match ? decodeURIComponent(match[1]) : null;
    }

    normalizeLocale(locale) {
        if (!locale) return I18nManager.DEFAULT_LOCALE;
        const t = locale.trim();
        if (t === 'pt' || t.toLowerCase() === 'pt-br') return I18nManager.DEFAULT_LOCALE;
        if (t === 'en' || t === 'es') return t;
        return I18nManager.DEFAULT_LOCALE;
    }

    flagPath(locale) {
        if (locale === 'en') return '/images/flags/en.svg';
        if (locale === 'es') return '/images/flags/es.svg';
        return '/images/flags/br.svg';
    }

    applyDocumentLang() {
        const lang = this.locale === 'en' ? 'en' : this.locale === 'es' ? 'es' : 'pt-BR';
        document.documentElement.lang = lang;
    }

    async ensureLoaded() {
        if (this.locale === I18nManager.DEFAULT_LOCALE) {
            this.messages = {};
            return;
        }
        if (this.messages) return;
        const response = await fetch('/i18n/messages/' + encodeURIComponent(this.locale) + '.json', {
            credentials: 'same-origin',
        });
        if (!response.ok) {
            console.warn('i18n: failed to load messages for', this.locale);
            this.messages = {};
            return;
        }
        this.messages = await response.json();
    }

    defaultLocaleText(key) {
        const seed = document.querySelector('#i18n-seeds [data-i18n="' + key + '"]');
        if (seed) {
            return seed.textContent?.trim() || null;
        }
        const anchor = document.querySelector('[data-i18n="' + key + '"]');
        return anchor?.textContent?.trim() || null;
    }

    t(key, params) {
        if (!key) return '';
        if (this.locale === I18nManager.DEFAULT_LOCALE) {
            const raw = this.defaultLocaleText(key);
            return raw ? this.interpolate(raw, params) : key;
        }
        const raw = this.messages?.[key];
        if (!raw) return key;
        return this.interpolate(raw, params);
    }

    interpolate(text, params) {
        if (!text || !params) return text || '';
        return text.replace(/\{(\w+)\}/g, (_, name) => {
            return params[name] != null ? String(params[name]) : '{' + name + '}';
        });
    }

    parseParams(el) {
        const raw = el.getAttribute('data-i18n-params');
        if (!raw) return null;
        try {
            return JSON.parse(raw);
        } catch (e) {
            console.warn('i18n: invalid data-i18n-params on', el);
            return null;
        }
    }

    isFormControl(el) {
        const tag = el.tagName;
        return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';
    }

    shouldSkipTextUpdate(el) {
        if (this.isFormControl(el)) {
            return true;
        }
        if (el.hasAttribute('data-i18n-attr') && !el.querySelector('[data-i18n-text]')) {
            return true;
        }
        return false;
    }

    applyText(el, translated) {
        const textTarget = el.querySelector('[data-i18n-text]');
        if (textTarget) {
            textTarget.textContent = translated;
            return;
        }
        if (el.children.length > 0) {
            return;
        }
        el.textContent = translated;
    }

    apply(root) {
        if (!root) return;
        if (this.locale === I18nManager.DEFAULT_LOCALE) return;
        if (!this.messages) return;

        root.querySelectorAll('[data-i18n]').forEach((el) => {
            if (this.shouldSkipTextUpdate(el)) {
                return;
            }
            const key = el.getAttribute('data-i18n');
            const translated = this.t(key, this.parseParams(el));
            if (translated && translated !== key) {
                this.applyText(el, translated);
            }
        });

        root.querySelectorAll('[data-i18n-attr]').forEach((el) => {
            const key = el.getAttribute('data-i18n');
            if (!key) return;
            const params = this.parseParams(el);
            const translated = this.t(key, params);
            const attrs = el.getAttribute('data-i18n-attr').split(':');
            attrs.forEach((attr) => {
                const name = attr.trim();
                if (name) el.setAttribute(name, translated);
            });
        });
    }

    syncLocalePickers(normalized) {
        const locale = normalized ?? this.locale;
        document.querySelectorAll('[data-locale-picker]').forEach((picker) => {
            const flag = picker.querySelector('[data-locale-flag]');
            if (flag) {
                flag.src = this.flagPath(locale);
            }
            picker.querySelectorAll('[data-locale]').forEach((opt) => {
                const active = opt.dataset.locale === locale;
                opt.classList.toggle('locale-picker__option--active', active);
                opt.setAttribute('aria-selected', active ? 'true' : 'false');
            });
        });
    }

    registerHtmxHooks() {
        document.body.addEventListener('htmx:afterSettle', (evt) => {
            const target = evt.detail?.target;
            if (target) {
                this.apply(target);
            } else {
                this.apply(document);
            }
            this.syncLocalePickers();
        });
    }

    async setLocale(locale) {
        const normalized = this.normalizeLocale(locale);
        await fetch('/forms/locale', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'X-CSRF-Token': document.querySelector('meta[name="csrf-token"]')?.content || '',
            },
            body: 'locale=' + encodeURIComponent(normalized),
            credentials: 'same-origin',
        });
        this.locale = normalized;
        this.messages = null;
        this.ready = false;
        document.documentElement.dataset.i18nReady = 'false';
        const meta = document.querySelector('meta[name="app-locale"]');
        if (meta) meta.content = normalized;
        await this.ensureLoaded();
        this.ready = true;
        document.documentElement.dataset.i18nReady = 'true';
        this.applyDocumentLang();
        this.apply(document);
        this.syncLocalePickers(normalized);
        document.querySelectorAll('.locale-picker__dropdown--open').forEach((panel) => {
            panel.classList.remove('locale-picker__dropdown--open');
        });
        document.querySelectorAll('.locale-picker__trigger[aria-expanded="true"]').forEach((btn) => {
            btn.setAttribute('aria-expanded', 'false');
        });
    }

    waitUntilReady() {
        if (this.ready) return Promise.resolve();
        return this.loadPromise || Promise.resolve();
    }
}

function initI18n() {
    if (window.i18n) {
        return window.i18n.loadPromise;
    }
    window.i18n = new I18nManager();
    return window.i18n.init();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initI18n);
} else {
    initI18n();
}
