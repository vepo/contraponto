/**
 * Toast notification manager for HTMX-driven UI.
 * Displays a non-intrusive message that auto-hides after a duration.
 * Can be triggered by:
 * - Response header "X-Toast-Message" (via htmx:afterRequest / htmx:afterSettle)
 * - HX-Trigger / custom event "toast:show" with detail { message, duration, type }
 */
class ToastManager {
    constructor(defaultDuration = 15000) {
        this.defaultDuration = defaultDuration;
        this.toastElement = null;
        this.timeoutId = null;
        this.init();
    }

    getToastElement() {
        if (this.toastElement && document.body.contains(this.toastElement)) {
            return this.toastElement;
        }

        const existingToast = document.getElementById('toast');
        if (existingToast) {
            this.toastElement = existingToast;
            return this.toastElement;
        }

        this.toastElement = document.createElement('div');
        this.toastElement.id = 'toast';
        this.toastElement.className = 'toast u-hidden';
        this.toastElement.innerHTML = `
            <div class="toast__content">
                <span id="toastMessage"></span>
            </div>
        `;
        document.body.appendChild(this.toastElement);
        return this.toastElement;
    }

    show(message, duration = this.defaultDuration, type) {
        if (!message) return;

        const toast = this.getToastElement();
        while (toast.firstChild) {
            toast.removeChild(toast.firstChild);
        }

        const wrapper = document.createElement('div');
        if (type === 'Success') {
            wrapper.className = 'toast--success';
        } else if (type === 'Error') {
            wrapper.className = 'toast--error';
        }
        wrapper.textContent = message;
        toast.appendChild(wrapper);

        if (this.timeoutId) {
            clearTimeout(this.timeoutId);
            this.timeoutId = null;
        }

        toast.classList.remove('u-hidden');
        toast.classList.add('toast--visible');

        this.timeoutId = setTimeout(() => {
            this.hide();
        }, duration);
    }

    hide() {
        const toast = this.getToastElement();
        toast.classList.add('u-hidden');
        toast.classList.remove('toast--visible');
        if (this.timeoutId) {
            clearTimeout(this.timeoutId);
            this.timeoutId = null;
        }
    }

    init() {
        this.getToastElement();
    }
}

function resolveToastMessage(detail, xhr) {
    const i18nKey = detail?.i18nKey || xhr?.getResponseHeader('X-Toast-I18n-Key');
    const fallback = detail?.message || xhr?.getResponseHeader('X-Toast-Message');
    if (i18nKey && window.i18n) {
        if (window.i18n.locale === 'pt-BR' && fallback) {
            return fallback;
        }
        let params = detail?.i18nParams;
        if (!params) {
            const paramsHeader = xhr?.getResponseHeader('X-Toast-I18n-Params');
            if (paramsHeader) {
                try {
                    params = JSON.parse(paramsHeader);
                } catch (e) {
                    params = null;
                }
            }
        }
        const translated = window.i18n.t(i18nKey, params);
        return translated !== i18nKey ? translated : (fallback || i18nKey);
    }
    return fallback;
}

function showToastFromXhr(event) {
    const xhr = event.detail?.xhr;
    if (!xhr || !window.toastManager) return;

    const toastMessage = resolveToastMessage(null, xhr);
    if (!toastMessage) return;

    let duration = window.toastManager.defaultDuration;
    const toastDuration = xhr.getResponseHeader('X-Toast-Duration');
    const toastType = xhr.getResponseHeader('X-Toast-Type');
    if (toastDuration && !isNaN(parseInt(toastDuration, 10))) {
        duration = parseInt(toastDuration, 10);
    }
    window.toastManager.show(toastMessage, duration, toastType);
}

async function showToastFromEvent(event) {
    if (!window.toastManager) return;
    const detail = event.detail || {};
    if (window.i18n) {
        await window.i18n.waitUntilReady();
    }
    const message = resolveToastMessage(detail, null);
    if (!message) return;
    window.toastManager.show(
        message,
        detail.duration || window.toastManager.defaultDuration,
        detail.type
    );
}

function registerToastListeners() {
    if (window.__toastListenersRegistered) {
        return;
    }
    window.__toastListenersRegistered = true;
    const capture = true;
    document.addEventListener('toast:show', showToastFromEvent, capture);
    document.addEventListener('htmx:afterRequest', showToastFromXhr, capture);
    document.addEventListener('htmx:afterSettle', showToastFromXhr, capture);
}

function initToastManager() {
    if (window.toastManager) {
        return;
    }
    window.toastManager = new ToastManager();
    window.showToast = (message, duration, type) => window.toastManager.show(message, duration, type);
    registerToastListeners();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initToastManager);
} else {
    initToastManager();
}
