/**
 * In-app confirm modal lifecycle for HTMX-driven mutations.
 * Clears #modal-container after a successful confirm, Cancel, or Escape.
 * Hub-shell redirect responses also clear the modal via OOB swap (seo-oob.html).
 */
class ConfirmModalManager {
    static MODAL_ID = 'confirmModal';
    static CONTAINER_ID = 'modal-container';
    static CONFIRM_SUBMIT_SELECTOR = '[data-confirm-submit]';
    static CLOSE_SELECTOR = '[data-confirm-modal-close]';

    constructor() {
        this.pendingClose = false;
        this.onClick = this.onClick.bind(this);
        this.onKeyDown = this.onKeyDown.bind(this);
        this.onAfterOnLoad = this.onAfterOnLoad.bind(this);
        this.onAfterSettle = this.onAfterSettle.bind(this);
        this.onAfterSwap = this.onAfterSwap.bind(this);
        document.addEventListener('click', this.onClick);
        document.addEventListener('keydown', this.onKeyDown);
        document.body.addEventListener('htmx:afterOnLoad', this.onAfterOnLoad);
        document.body.addEventListener('htmx:afterSettle', this.onAfterSettle);
        document.body.addEventListener('htmx:afterSwap', this.onAfterSwap);
    }

    close() {
        this.pendingClose = false;
        const container = document.getElementById(ConfirmModalManager.CONTAINER_ID);
        if (container) {
            container.innerHTML = '';
        }
    }

    hasOpenModal() {
        return document.getElementById(ConfirmModalManager.MODAL_ID) != null;
    }

    onAfterOnLoad(event) {
        if (!this.shouldCloseAfterRequest(event)) {
            return;
        }
        if (event.detail.successful) {
            this.close();
        }
    }

    onAfterSettle(event) {
        if (!this.pendingClose || !this.hasOpenModal()) {
            return;
        }
        if (event.detail.successful) {
            this.close();
            return;
        }
        this.pendingClose = false;
    }

    onAfterSwap(event) {
        const target = event.detail?.target;
        if (target?.id !== ConfirmModalManager.CONTAINER_ID) {
            return;
        }
        window.i18n?.apply(target);
    }

    onClick(event) {
        if (event.target.closest(ConfirmModalManager.CLOSE_SELECTOR)) {
            event.preventDefault();
            this.close();
            return;
        }
        if (event.target.closest(ConfirmModalManager.CONFIRM_SUBMIT_SELECTOR)) {
            this.pendingClose = true;
        }
    }

    onKeyDown(event) {
        if (event.key !== 'Escape' || !this.hasOpenModal()) {
            return;
        }
        event.preventDefault();
        this.close();
    }

    shouldCloseAfterRequest(event) {
        const elt = event.detail?.elt;
        if (!elt?.hasAttribute?.('data-confirm-submit')) {
            return false;
        }
        return this.hasOpenModal();
    }
}

function registerConfirmModalManager() {
    if (window.__confirmModalManagerRegistered) {
        return;
    }
    window.__confirmModalManagerRegistered = true;
    new ConfirmModalManager();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', registerConfirmModalManager);
} else {
    registerConfirmModalManager();
}
