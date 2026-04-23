/**
 * Toast notification manager for HTMX-driven UI.
 * Displays a non-intrusive message that auto-hides after a duration.
 * Can be triggered by:
 * - Response header "X-Toast-Message"
 * - Custom event "toast:show" with detail { message, duration }
 */
class ToastManager {
    constructor(defaultDuration = 15000) {
        this.defaultDuration = defaultDuration;
        this.toastElement = null;
        this.timeoutId = null;
        this.init();
        this.setupHtmxListener();
        this.setupCustomEventListener();
    }

    /**
     * Create or get the toast DOM element.
     */
    getToastElement() {
        if (this.toastElement && document.body.contains(this.toastElement)) {
            return this.toastElement;
        }

        // Try to find existing toast element
        const existingToast = document.getElementById('toast');
        if (existingToast) {
            this.toastElement = existingToast;
            return this.toastElement;
        }

        // Create new toast element if not present
        this.toastElement = document.createElement('div');
        this.toastElement.id = 'toast';
        this.toastElement.className = 'toast';
        this.toastElement.style.display = 'none';
        this.toastElement.innerHTML = `
            <div class="toast__content">
                <span id="toastMessage"></span>
            </div>
        `;
        document.body.appendChild(this.toastElement);
        return this.toastElement;
    }

    /**
     * Show a toast message.
     * @param {string} message - The message to display.
     * @param {number} duration - Milliseconds to show (default 15000).
     */
    show(message, duration = this.defaultDuration, type) {
        if (!message) return;

        const toast = this.getToastElement();
        while(toast.firstChild) {
            toast.removeChild(toast.firstChild);
        }

        if (type == 'Success') {
            toast.innerHTML = `<div class="toast--success">${message}</div>`
        } else if (type == 'Error') {
            toast.innerHTML = `<div class="toast--error">${message}</div>`
        } else {
            toast.innerHTML = message
        }

        // Clear any pending hide timeout
        if (this.timeoutId) {
            clearTimeout(this.timeoutId);
            this.timeoutId = null;
        }

        // Show the toast
        toast.style.display = 'block';
        toast.classList.add('toast--visible');

        // Auto-hide after duration
        this.timeoutId = setTimeout(() => {
            this.hide();
        }, duration);
    }

    /**
     * Hide the toast immediately.
     */
    hide() {
        const toast = this.getToastElement();
        toast.style.display = 'none';
        toast.classList.remove('toast--visible');
        if (this.timeoutId) {
            clearTimeout(this.timeoutId);
            this.timeoutId = null;
        }
    }

    /**
     * Set up HTMX listener to capture response headers.
     * When a response includes "X-Toast-Message" header, show the toast.
     */
    setupHtmxListener() {
        document.body.addEventListener('htmx:afterRequest', (event) => {
            const xhr = event.detail.xhr;
            if (!xhr) return;

            const toastMessage = xhr.getResponseHeader('X-Toast-Message');
            if (toastMessage) {
                // Optional: also check for custom duration header
                let duration = this.defaultDuration;
                const toastDuration = xhr.getResponseHeader('X-Toast-Duration');
                const toastType = xhr.getResponseHeader('X-Toast-Type');
                if (toastDuration && !isNaN(parseInt(toastDuration))) {
                    duration = parseInt(toastDuration);
                }
                this.show(toastMessage, duration, toastType);
            }
        });
    }

    /**
     * Set up custom event listener for programmatic toasts.
     * Usage: 
     *   const event = new CustomEvent('toast:show', { detail: { message: 'Hello!', duration: 5000 } });
     *   document.dispatchEvent(event);
     */
    setupCustomEventListener() {
        document.addEventListener('toast:show', (event) => {
            const { message, duration } = event.detail || {};
            if (message) {
                this.show(message, duration || this.defaultDuration);
            }
        });
    }

    /**
     * Initialize any existing toast element.
     */
    init() {
        // If a toast element already exists, keep it
        this.getToastElement();
    }
}

// Create a global instance when DOM is ready
let toastManager = null;
document.addEventListener('DOMContentLoaded', () => {
    toastManager = new ToastManager();
    // Expose globally for easy debugging or manual calls
    window.toastManager = toastManager;
    window.showToast = (message, duration) => toastManager.show(message, duration);
});