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

        if (type === 'Success') {
            toast.innerHTML = `<div class="toast--success">${message}</div>`;
        } else if (type === 'Error') {
            toast.innerHTML = `<div class="toast--error">${message}</div>`;
        } else {
            toast.innerHTML = message;
        }

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

function showToastFromXhr(event) {
    const xhr = event.detail?.xhr;
    if (!xhr || !window.toastManager) return;

    const toastMessage = xhr.getResponseHeader('X-Toast-Message');
    if (!toastMessage) return;

    let duration = window.toastManager.defaultDuration;
    const toastDuration = xhr.getResponseHeader('X-Toast-Duration');
    const toastType = xhr.getResponseHeader('X-Toast-Type');
    if (toastDuration && !isNaN(parseInt(toastDuration, 10))) {
        duration = parseInt(toastDuration, 10);
    }
    window.toastManager.show(toastMessage, duration, toastType);
}

function showToastFromEvent(event) {
    if (!window.toastManager) return;
    const { message, duration, type } = event.detail || {};
    if (!message) return;
    window.toastManager.show(
        message,
        duration || window.toastManager.defaultDuration,
        type
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
