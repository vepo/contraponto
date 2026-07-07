class FormsManager {
    constructor() {
        this.configureFormsElements = this.configureFormsElements.bind(this);
        this.setupGlobalValidationListener();
        this.setupModalListener();
        this.setupUserDeactivateConfirm();
        this.setupForms();
    }

    setupForms() {
        this.configureFormsElements();
        document.body.addEventListener('htmx:afterSettle', this.configureFormsElements);
    }

    configurePristineElement(elm) {
        if (elm.pristine === undefined) {
            elm.pristine = true;
            let hasChanged = false;
            const inputHandler = () => {
                hasChanged = true;
            };
            const blurHandler = () => {
                if (hasChanged) {
                    elm.pristine = false;
                    // Optionally remove listeners to avoid further changes
                    elm.removeEventListener('input', inputHandler);
                    elm.removeEventListener('blur', blurHandler);
                }
            };
            elm.addEventListener('keydown', inputHandler);
            elm.addEventListener('input', inputHandler);
            elm.addEventListener('blur', blurHandler);
        }
    }

    configureFormsElements(evt) {
        if (!evt) {
            const target = document.querySelector('form');
            if (target) {
                for (const input of target.querySelectorAll('input')) {
                    console.log('Input', input);
                    this.configurePristineElement(input);
                }
            }
        }
        if (evt && evt.detail.target) {
            const target = evt.detail.target;
            for (const input of target.querySelectorAll('input')) {
                console.log('Input', input);
                this.configurePristineElement(input);
            }
        }
    }

    // Global listener – handles validation for any auth modal present in DOM
    setupGlobalValidationListener() {
        const validateForm = (form) => {
            if (!form) return;

            const submitBtn = form.querySelector('button[type="submit"]');
            const inputs = form.querySelectorAll('input');
            if (inputs.length == 0 || !submitBtn) {
                return;
            }

            // Reset all error messages inside the modal
            const allErrors = form.querySelectorAll('.error-message');
            allErrors.forEach(err => err.classList.remove('visible'));

            let isValid = true;

            for (const input of inputs) {
                if (input.required) {
                    const value = input.value;
                    if (!value) {
                        if (input.pristine != undefined && !input.pristine) {
                            const errorMessage = input.closest('.form-group')?.querySelector('.error-message.required');
                            if (errorMessage) errorMessage.classList.add('visible');
                        }
                        isValid = false;
                    }
                }

                if (input.hasAttribute('email')) {
                    const value = input.value;
                    if (value && value.length > 0) { // value.length == 0 is validated on required
                        const email = input.value.trim();
                        const emailRegex = /^[^\s@]+@([^\s@]+\.)+[^\s@]+$/;
                        if (!emailRegex.test(email)) {
                            if (input.pristine != undefined && !input.pristine) {
                                const errorMessage = input.closest('.form-group')?.querySelector('.error-message.email');
                                if (errorMessage) errorMessage.classList.add('visible');
                            }
                            isValid = false;
                        }
                    }
                }

                if (input.hasAttribute('pattern')) {
                    const value = input.value;
                    if (value && value.length > 0) {
                        const pattern = new RegExp('^(?:' + input.getAttribute('pattern') + ')$');
                        if (!pattern.test(value)) {
                            if (input.pristine != undefined && !input.pristine) {
                                const errorMessage = input.closest('.form-group')?.querySelector('.error-message.pattern');
                                if (errorMessage) errorMessage.classList.add('visible');
                            }
                            isValid = false;
                        }
                    }
                }

                if (input.hasAttribute('min-size') || input.hasAttribute('max-size')) {
                    const value = input.value;
                    if (value && value.length > 0) { // value.length == 0 is validated on required
                        const minSize = input.getAttribute('min-size');
                        if (minSize) {
                            const minSizeValue = parseInt(minSize);
                            if (Number.isNaN(minSizeValue)) {
                                console.warn("'min-value' is NaN", input);
                            } else {
                                if (value.trim().length < minSizeValue) {
                                    if (input.pristine != undefined && !input.pristine) {
                                        const errorMessage = input.closest('.form-group')?.querySelector('.error-message.min-value');
                                        if (errorMessage) errorMessage.classList.add('visible');
                                    }
                                    isValid = false;
                                }
                            }
                        }

                        const maxSize = input.getAttribute('max-size');
                        if (maxSize) {
                            const maxSizeValue = parseInt(maxSize);
                            if (Number.isNaN(maxSizeValue)) {
                                console.warn("'max-value' is NaN", input);
                            } else {
                                if (value.trim().length > maxSizeValue) {
                                    if (input.pristine != undefined && !input.pristine) {
                                        const errorMessage = input.closest('.form-group')?.querySelector('.error-message.max-value');
                                        if (errorMessage) errorMessage.classList.add('visible');
                                    }
                                    isValid = false;
                                }
                            }
                        }
                    }
                }
            }

            submitBtn.disabled = !isValid;
        };

        // Trigger validation on any input/change inside the auth modal
        document.body.addEventListener('input', (e) => {
            if (e.target.closest('form')) validateForm(e.target.closest('form'));
        });
        document.body.addEventListener('change', (e) => {
            if (e.target.closest('form')) validateForm(e.target.closest('form'));
        });
    }

    // Re‑validate after HTMX loads a new modal (e.g., after switching login/signup)
    setupModalListener() {
        document.querySelectorAll('form').forEach(form => {
            if (form) {
                // Manually trigger validation to set initial button state
                const event = new Event('input', { bubbles: true });
                const firstInput = form.querySelector('input');
                if (firstInput) {
                    firstInput.dispatchEvent(event);
                    firstInput.focus();
                }
            }
        })
        document.body.addEventListener("htmx:afterSettle", (evt) => {
            if (evt && evt.detail && evt.detail.target && evt.detail.target.querySelector('form')) {
                // Manually trigger validation to set initial button state
                const event = new Event('input', { bubbles: true });
                const firstInput = evt.detail.target.querySelector('form input');
                if (firstInput) {
                    firstInput.dispatchEvent(event);
                    firstInput.focus();
                }
            }
        });
    }

    setupUserDeactivateConfirm() {
        let pendingRequest = null;

        const closeModal = () => {
            const modal = document.getElementById('userDeactivateModal');
            if (modal) {
                modal.classList.remove('modal--open');
                modal.setAttribute('aria-hidden', 'true');
            }
            pendingRequest = null;
        };

        document.body.addEventListener('htmx:confirm', (event) => {
            const form = event.detail?.elt?.closest?.('form.js-user-manage-form');
            if (!form || form.dataset.wasActive !== 'true') {
                return;
            }
            const active = form.querySelector('.user-status__toggle-input');
            if (!active || active.checked) {
                return;
            }
            event.preventDefault();
            pendingRequest = event.detail;
            const modal = document.getElementById('userDeactivateModal');
            if (modal) {
                modal.classList.add('modal--open');
                modal.setAttribute('aria-hidden', 'false');
            }
        });

        document.body.addEventListener('click', (event) => {
            if (event.target.closest('[data-user-deactivate-cancel]')) {
                event.preventDefault();
                closeModal();
                return;
            }
            if (event.target.closest('[data-user-deactivate-confirm]')) {
                event.preventDefault();
                if (pendingRequest?.issueRequest) {
                    pendingRequest.issueRequest(true);
                }
                closeModal();
            }
        });

        document.body.addEventListener('keydown', (event) => {
            if (event.key === 'Escape' && document.getElementById('userDeactivateModal')?.classList.contains('modal--open')) {
                event.preventDefault();
                closeModal();
            }
        });
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new FormsManager();
});