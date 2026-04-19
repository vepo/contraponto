class ModalManager {
    constructor() {
        this.configureFormsElements = this.configureFormsElements.bind(this);
        this.setupGlobalValidationListener();
        this.setupModalListener();
        this.setupForms();
    }

    setupForms() {
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
        if (evt.detail.target) {
            const target = evt.detail.target;
            for (const input of target.querySelectorAll('input')) {
                console.log('Input', input);
                this.configurePristineElement(input);
            }
        }
    }

    // Global listener – handles validation for any auth modal present in DOM
    setupGlobalValidationListener() {
        const validateModal = () => {
            const modal = document.getElementById("authModal");
            if (!modal) return;

            const submitBtn = modal.querySelector('button[type="submit"]');
            const inputs = modal.querySelectorAll('input');
            if (inputs.length == 0 || !submitBtn) {
                return;
            }

            // Reset all error messages inside the modal
            const allErrors = modal.querySelectorAll('.error-message');
            allErrors.forEach(err => err.style.display = 'none');

            let isValid = true;

            for (const input of inputs) {
                if (input.required) {
                    const value = input.value;
                    if (!value) {
                        if (!input.pristine) {
                            const errorMessage = input.closest('.form-group')?.querySelector('.error-message.required');
                            if (errorMessage) errorMessage.style.display = 'block';
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
                            if (!input.pristine) {
                                const errorMessage = input.closest('.form-group')?.querySelector('.error-message.email');
                                if (errorMessage) errorMessage.style.display = 'block';
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
                                    if (!input.pristine) {
                                        const errorMessage = input.closest('.form-group')?.querySelector('.error-message.min-value');
                                        if (errorMessage) errorMessage.style.display = 'block';
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
                                    if (!input.pristine) {
                                        const errorMessage = input.closest('.form-group')?.querySelector('.error-message.max-value');
                                        if (errorMessage) errorMessage.style.display = 'block';
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
            if (e.target.closest('#authModal')) validateModal();
        });
        document.body.addEventListener('change', (e) => {
            if (e.target.closest('#authModal')) validateModal();
        });
    }

    // Re‑validate after HTMX loads a new modal (e.g., after switching login/signup)
    setupModalListener() {
        document.body.addEventListener("htmx:afterSettle", (evt) => {
            if (evt.detail.target.id === "authModal") {
                // Manually trigger validation to set initial button state
                const event = new Event('input', { bubbles: true });
                const loginInput = evt.detail.target.querySelector('input[name="login"]');
                if (loginInput) loginInput.dispatchEvent(event);
            }
        });
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new ModalManager();
});