class ConfirmModalManager {
    constructor() {
        this.onClick = this.onClick.bind(this);
        this.onKeyDown = this.onKeyDown.bind(this);
        this.onAfterOnLoad = this.onAfterOnLoad.bind(this);
        this.onAfterSwap = this.onAfterSwap.bind(this);
        document.addEventListener('click', this.onClick);
        document.addEventListener('keydown', this.onKeyDown);
        document.body.addEventListener('htmx:afterOnLoad', this.onAfterOnLoad);
        document.body.addEventListener('htmx:afterSwap', this.onAfterSwap);
    }

    close() {
        var container = document.getElementById('modal-container');
        if (container) {
            container.innerHTML = '';
        }
    }

    onAfterOnLoad(event) {
        var elt = event.detail?.elt;
        if (!elt?.hasAttribute?.('data-confirm-submit')) {
            return;
        }
        if (!document.getElementById('confirmModal')) {
            return;
        }
        if (event.detail.successful) {
            this.close();
        }
    }

    onAfterSwap(event) {
        var target = event.detail?.target;
        if (target?.id !== 'modal-container') {
            return;
        }
        window.i18n?.apply(target);
    }

    onClick(event) {
        if (event.target.closest('[data-confirm-modal-close]')) {
            event.preventDefault();
            this.close();
        }
    }

    onKeyDown(event) {
        if (event.key === 'Escape' && document.getElementById('confirmModal')) {
            event.preventDefault();
            this.close();
        }
    }
}

document.addEventListener('DOMContentLoaded', function () {
    new ConfirmModalManager();
});
