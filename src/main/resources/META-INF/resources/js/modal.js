class ModalManager {
    constructor() {
        this.setupGlobalValidationListener();
        this.setupModalListener();
    }

    // Global listener – handles validation for any auth modal present in DOM
    setupGlobalValidationListener() {
        const validateModal = () => {
            const modal = document.getElementById("authModal");
            if (!modal) return;

            const submitBtn = modal.querySelector('button[type="submit"]');
            const emailInput = modal.querySelector('input[name="email"]');
            const passwordInput = modal.querySelector('input[name="password"]');

            if (!submitBtn || !emailInput || !passwordInput) return;

            // Reset all error messages inside the modal
            const allErrors = modal.querySelectorAll('.error-message');
            allErrors.forEach(err => err.style.display = 'none');

            let isValid = true;

            // Email validation
            const email = emailInput.value.trim();
            const emailRegex = /^[^\s@]+@([^\s@]+\.)+[^\s@]+$/;
            if (!email || !emailRegex.test(email)) {
                isValid = false;
                const emailError = emailInput.closest('.form-group')?.querySelector('.error-message');
                if (emailError) emailError.style.display = 'block';
            }

            // Password validation (non‑empty)
            const password = passwordInput.value;
            if (!password) {
                isValid = false;
                const passwordError = passwordInput.closest('.form-group')?.querySelector('.error-message');
                if (passwordError) passwordError.style.display = 'block';
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
                const emailInput = evt.detail.target.querySelector('input[name="email"]');
                if (emailInput) emailInput.dispatchEvent(event);
            }
        });
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new ModalManager();
});