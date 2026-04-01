class AuthManager {
    constructor() {
        this.currentUser = null;
        this.isAuthenticated = false;
        this.isSignupMode = false;
        this.apiUrl = '/api/auth';
        this.init();
    }

    init() {
        this.loadStoredSession();
        this.setupEventListeners();
        this.renderAuthUI();
    }

    loadStoredSession() {
        const storedUser = localStorage.getItem('contraponto_user');
        const storedToken = localStorage.getItem('contraponto_token');

        if (storedUser && storedToken) {
            this.currentUser = JSON.parse(storedUser);
            this.isAuthenticated = true;
        }
    }

    setupEventListeners() {
        // Modal triggers
        const authSection = document.getElementById('authSection');
        const modal = document.getElementById('authModal');
        const closeBtn = document.getElementById('closeModalBtn');
        const switchBtn = document.getElementById('switchModeBtn');
        const authForm = document.getElementById('authForm');

        if (closeBtn) {
            closeBtn.addEventListener('click', () => this.closeModal());
        }

        if (switchBtn) {
            switchBtn.addEventListener('click', () => this.toggleAuthMode());
        }

        if (authForm) {
            authForm.addEventListener('submit', (e) => this.handleAuthSubmit(e));
        }

        // Close modal on outside click
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) this.closeModal();
            });
        }

        // Escape key to close modal
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && modal?.classList.contains('modal--open')) {
                this.closeModal();
            }
        });
    }

    openModal() {
        const modal = document.getElementById('authModal');
        if (modal) {
            modal.classList.add('modal--open');
            this.resetForm();
        }
    }

    closeModal() {
        const modal = document.getElementById('authModal');
        if (modal) {
            modal.classList.remove('modal--open');
            this.resetForm();
        }
    }

    resetForm() {
        const form = document.getElementById('authForm');
        const errorDiv = document.getElementById('authError');
        const successDiv = document.getElementById('authSuccess');

        if (form) form.reset();
        if (errorDiv) errorDiv.style.display = 'none';
        if (successDiv) successDiv.style.display = 'none';

        this.isSignupMode = false;
        this.updateModalForMode();
    }

    toggleAuthMode() {
        this.isSignupMode = !this.isSignupMode;
        this.updateModalForMode();
        this.clearMessages();
    }

    updateModalForMode() {
        const modalTitle = document.getElementById('modalTitle');
        const submitBtn = document.getElementById('submitBtn');
        const switchText = document.getElementById('switchText');
        const switchBtn = document.getElementById('switchModeBtn');
        const signupFields = document.getElementById('signupFields');

        if (this.isSignupMode) {
            if (modalTitle) modalTitle.textContent = 'Create Account';
            if (submitBtn) submitBtn.textContent = 'Sign Up';
            if (switchText) switchText.textContent = 'Already have an account?';
            if (switchBtn) switchBtn.textContent = 'Sign In';
            if (signupFields) signupFields.style.display = 'block';
        } else {
            if (modalTitle) modalTitle.textContent = 'Sign In';
            if (submitBtn) submitBtn.textContent = 'Sign In';
            if (switchText) switchText.textContent = "Don't have an account?";
            if (switchBtn) switchBtn.textContent = 'Sign Up';
            if (signupFields) signupFields.style.display = 'none';
        }
    }

    clearMessages() {
        const errorDiv = document.getElementById('authError');
        const successDiv = document.getElementById('authSuccess');
        if (errorDiv) errorDiv.style.display = 'none';
        if (successDiv) successDiv.style.display = 'none';
    }

    showError(message) {
        const errorDiv = document.getElementById('authError');
        if (errorDiv) {
            errorDiv.textContent = message;
            errorDiv.style.display = 'block';
        }
    }

    showSuccess(message) {
        const successDiv = document.getElementById('authSuccess');
        if (successDiv) {
            successDiv.textContent = message;
            successDiv.style.display = 'block';
        }
    }

    async handleAuthSubmit(e) {
        e.preventDefault();
        this.clearMessages();

        const email = document.getElementById('email')?.value;
        const password = document.getElementById('password')?.value;

        if (!email || !password) {
            this.showError('Please fill in all fields');
            return;
        }

        if (this.isSignupMode) {
            const name = document.getElementById('name')?.value;
            if (!name) {
                this.showError('Please enter your name');
                return;
            }
            await this.signup(name, email, password);
        } else {
            await this.login(email, password);
        }
    }

    async login(email, password) {
        try {
            const response = await fetch(`${this.apiUrl}/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ email, password })
            });

            const data = await response.json();

            if (response.ok) {
                // Store session
                localStorage.setItem('contraponto_user', JSON.stringify(data.user));
                localStorage.setItem('contraponto_token', data.token);
                if (data.refreshToken) {
                    localStorage.setItem('contraponto_refresh_token', data.refreshToken);
                }

                this.currentUser = data.user;
                this.isAuthenticated = true;

                this.showSuccess('Successfully signed in!');

                setTimeout(() => {
                    this.closeModal();
                    this.renderAuthUI();
                    window.location.reload(); // Refresh to update UI
                }, 1000);
            } else {
                this.showError(data.error || 'Login failed. Please check your credentials.');
            }
        } catch (error) {
            console.error('Login error:', error);
            this.showError('Login failed. Please check your connection.');
        }
    }

    async signup(name, email, password) {
        try {
            const response = await fetch(`${this.apiUrl}/signup`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ name, email, password })
            });

            const data = await response.json();

            if (response.ok) {
                // Store session
                localStorage.setItem('contraponto_user', JSON.stringify(data.user));
                localStorage.setItem('contraponto_token', data.token);
                if (data.refreshToken) {
                    localStorage.setItem('contraponto_refresh_token', data.refreshToken);
                }

                this.currentUser = data.user;
                this.isAuthenticated = true;

                this.showSuccess('Account created successfully!');

                setTimeout(() => {
                    this.closeModal();
                    this.renderAuthUI();
                    window.location.reload(); // Refresh to update UI
                }, 1000);
            } else {
                this.showError(data.error || 'Signup failed. Please try again.');
            }
        } catch (error) {
            console.error('Signup error:', error);
            this.showError('Signup failed. Please check your connection.');
        }
    }

    logout() {
        // Call logout endpoint
        fetch(`${this.apiUrl}/logout`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('contraponto_token')}`
            }
        }).catch(err => console.error('Logout error:', err));

        // Clear local storage
        localStorage.removeItem('contraponto_user');
        localStorage.removeItem('contraponto_token');
        localStorage.removeItem('contraponto_refresh_token');

        this.currentUser = null;
        this.isAuthenticated = false;
        this.renderAuthUI();
        window.location.reload(); // Refresh to update UI
    }

    renderAuthUI() {
        const authSection = document.getElementById('authSection');
        if (!authSection) return;

        if (this.isAuthenticated && this.currentUser) {
            // Render user menu (single element)
            const avatarInitial = this.currentUser.name.charAt(0).toUpperCase();
            const avatarUrl = this.currentUser.avatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(this.currentUser.name)}&background=1a8917&color=fff&bold=true`;

            authSection.innerHTML = `
            <div class="user-menu">
                <button class="user-menu__button" id="userMenuBtn">
                    <img src="${avatarUrl}" alt="${this.currentUser.name}" class="user-menu__avatar" onerror="this.src='https://ui-avatars.com/api/?name=${avatarInitial}&background=1a8917&color=fff'"/>
                    <span>${this.currentUser.name.split(' ')[0]}</span>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <path d="M6 9L12 15L18 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                    </svg>
                </button>
                <div class="user-menu__dropdown" id="userDropdown">
                    <div class="user-menu__header">
                        <div class="user-menu__name">${this.currentUser.name}</div>
                        <div class="user-menu__email">${this.currentUser.email}</div>
                    </div>
                    <div class="user-menu__items">
                        <a href="/profile" class="user-menu__item">
                            <svg viewBox="0 0 24 24" fill="none" width="18" height="18">
                                <path d="M20 21V19C20 16.8 18.2 15 16 15H8C5.8 15 4 16.8 4 19V21M16 7C16 9.2 14.2 11 12 11C9.8 11 8 9.2 8 7C8 4.8 9.8 3 12 3C14.2 3 16 4.8 16 7Z" stroke="currentColor" stroke-width="1.5"/>
                            </svg>
                            Profile
                        </a>
                        <a href="/write" class="user-menu__item">
                            <svg viewBox="0 0 24 24" fill="none" width="18" height="18">
                                <path d="M12 4V20M4 12H20" stroke="currentColor" stroke-width="1.5"/>
                            </svg>
                            Write
                        </a>
                        <a href="/dashboard" class="user-menu__item">
                            <svg viewBox="0 0 24 24" fill="none" width="18" height="18">
                                <path d="M3 3H21V21H3V3ZM7 7H17V9H7V7ZM7 11H17V13H7V11ZM7 15H13V17H7V15Z" stroke="currentColor" stroke-width="1.5"/>
                            </svg>
                            Dashboard
                        </a>
                        <div class="user-menu__divider"></div>
                        <button class="user-menu__item" id="logoutBtn">
                            <svg viewBox="0 0 24 24" fill="none" width="18" height="18">
                                <path d="M9 21H5C4.5 21 4 20.5 4 20V4C4 3.5 4.5 3 5 3H9M16 17L21 12M21 12L16 7M21 12H9" stroke="currentColor" stroke-width="1.5"/>
                            </svg>
                            Sign out
                        </button>
                    </div>
                </div>
            </div>
        `;

            // Setup user menu dropdown
            const userMenuBtn = document.getElementById('userMenuBtn');
            const userDropdown = document.getElementById('userDropdown');
            const logoutBtn = document.getElementById('logoutBtn');

            if (userMenuBtn && userDropdown) {
                userMenuBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    userDropdown.classList.toggle('user-menu__dropdown--open');
                });

                // Close dropdown when clicking outside
                document.addEventListener('click', (e) => {
                    if (!userMenuBtn.contains(e.target) && !userDropdown.contains(e.target)) {
                        userDropdown.classList.remove('user-menu__dropdown--open');
                    }
                });
            }

            if (logoutBtn) {
                logoutBtn.addEventListener('click', () => this.logout());
            }

        } else {
            // Render login/signup buttons in a container
            authSection.innerHTML = `
            <div class="auth-buttons">
                <button class="auth-btn auth-btn--signup" id="signupBtn">Sign Up</button>
                <button class="auth-btn auth-btn--login" id="loginBtn">Sign In</button>
            </div>
        `;

            const signupBtn = document.getElementById('signupBtn');
            const loginBtn = document.getElementById('loginBtn');

            if (signupBtn) {
                signupBtn.addEventListener('click', () => {
                    this.isSignupMode = true;
                    this.updateModalForMode();
                    this.openModal();
                });
            }

            if (loginBtn) {
                loginBtn.addEventListener('click', () => {
                    this.isSignupMode = false;
                    this.updateModalForMode();
                    this.openModal();
                });
            }
        }
    }
}

// Initialize auth on page load
const authManager = new AuthManager();

// Make auth manager globally available for debugging (optional)
window.authManager = authManager;