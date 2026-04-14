class AuthManager {
    constructor() {
        this.currentUser = null;
        this.isAuthenticated = false;
        this.isSignupMode = false;
        this.apiUrl = '/api/auth';
        this.refreshPromise = null; // Prevent multiple refresh requests
        this.init();
    }

    init() {
        this.loadStoredSession();
        this.setupEventListeners();
        this.renderAuthUI();
        this.setupAuthRedirect(); // New: handle protected routes
    }

    loadStoredSession() {
        const storedUser = localStorage.getItem('contraponto_user');
        const storedToken = localStorage.getItem('contraponto_token');
        const tokenExpiry = localStorage.getItem('contraponto_token_expiry');

        if (storedUser && storedToken) {
            // Check if token is expired
            if (tokenExpiry && Date.now() > parseInt(tokenExpiry)) {
                console.log('Token expired, attempting refresh...');
                this.refreshToken().then(success => {
                    if (!success) {
                        this.logout();
                    }
                });
            } else {
                this.currentUser = JSON.parse(storedUser);
                this.isAuthenticated = true;
            }
        }
    }

    setupAuthRedirect() {
        // Check if current page requires authentication
        const protectedPaths = ['/profile', '/write', '/dashboard'];
        const currentPath = window.location.pathname;
        
        if (protectedPaths.includes(currentPath) && !this.isAuthenticated) {
            // Store intended destination
            localStorage.setItem('redirectAfterLogin', currentPath);
            this.openModal();
        }
    }

    setupEventListeners() {
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

        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) this.closeModal();
            });
        }

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
            document.body.style.overflow = 'hidden'; // Prevent background scroll
        }
    }

    closeModal() {
        const modal = document.getElementById('authModal');
        if (modal) {
            modal.classList.remove('modal--open');
            this.resetForm();
            document.body.style.overflow = '';
        }
    }

    resetForm() {
        const form = document.getElementById('authForm');
        const errorDiv = document.getElementById('authError');
        const successDiv = document.getElementById('authSuccess');

        if (form) form.reset();
        if (errorDiv) {
            errorDiv.style.display = 'none';
            errorDiv.textContent = '';
        }
        if (successDiv) {
            successDiv.style.display = 'none';
            successDiv.textContent = '';
        }

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
        const passwordField = document.getElementById('password');

        if (this.isSignupMode) {
            if (modalTitle) modalTitle.textContent = 'Create Account';
            if (submitBtn) submitBtn.textContent = 'Sign Up';
            if (switchText) switchText.textContent = 'Already have an account?';
            if (switchBtn) switchBtn.textContent = 'Sign In';
            if (signupFields) signupFields.style.display = 'block';
            if (passwordField) passwordField.autocomplete = 'new-password';
        } else {
            if (modalTitle) modalTitle.textContent = 'Sign In';
            if (submitBtn) submitBtn.textContent = 'Sign In';
            if (switchText) switchText.textContent = "Don't have an account?";
            if (switchBtn) switchBtn.textContent = 'Sign Up';
            if (signupFields) signupFields.style.display = 'none';
            if (passwordField) passwordField.autocomplete = 'current-password';
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
            // Auto-hide after 5 seconds
            setTimeout(() => {
                if (errorDiv.style.display === 'block') {
                    errorDiv.style.display = 'none';
                }
            }, 5000);
        }
    }

    showSuccess(message) {
        const successDiv = document.getElementById('authSuccess');
        if (successDiv) {
            successDiv.textContent = message;
            successDiv.style.display = 'block';
            // Auto-hide after 3 seconds
            setTimeout(() => {
                if (successDiv.style.display === 'block') {
                    successDiv.style.display = 'none';
                }
            }, 3000);
        }
    }

    async handleAuthSubmit(e) {
        e.preventDefault();
        this.clearMessages();

        const email = document.getElementById('email')?.value.trim();
        const password = document.getElementById('password')?.value;

        if (!email || !password) {
            this.showError('Please fill in all fields');
            return;
        }

        // Basic email validation
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            this.showError('Please enter a valid email address');
            return;
        }

        if (password.length < 6) {
            this.showError('Password must be at least 6 characters');
            return;
        }

        // Disable submit button to prevent double submission
        const submitBtn = document.getElementById('submitBtn');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = this.isSignupMode ? 'Creating account...' : 'Signing in...';
        }

        try {
            if (this.isSignupMode) {
                const name = document.getElementById('name')?.value.trim();
                if (!name) {
                    this.showError('Please enter your name');
                    return;
                }
                if (name.length < 2) {
                    this.showError('Name must be at least 2 characters');
                    return;
                }
                await this.signup(name, email, password);
            } else {
                await this.login(email, password);
            }
        } finally {
            // Re-enable submit button
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = this.isSignupMode ? 'Sign Up' : 'Sign In';
            }
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
                if ( data && data.user && data.user.sessionId) {
                    document.cookie = `__session=${data.user.sessionId}`;
                }
                this.storeSession(data);
                this.showSuccess('Successfully signed in!');

                setTimeout(() => {
                    this.closeModal();
                    this.renderAuthUI();
                    
                    // Redirect to intended page if any
                    const redirectUrl = localStorage.getItem('redirectAfterLogin');
                    if (redirectUrl) {
                        localStorage.removeItem('redirectAfterLogin');
                        window.location.href = redirectUrl;
                    } else {
                        window.location.reload();
                    }
                }, 1000);
            } else {
                this.showError(data.error || data.message || 'Login failed. Please check your credentials.');
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
                this.storeSession(data);
                this.showSuccess('Account created successfully!');

                setTimeout(() => {
                    this.closeModal();
                    this.renderAuthUI();
                    window.location.reload();
                }, 1000);
            } else {
                this.showError(data.error || data.message || 'Signup failed. Please try again.');
            }
        } catch (error) {
            console.error('Signup error:', error);
            this.showError('Signup failed. Please check your connection.');
        }
    }

    storeSession(data) {
        localStorage.setItem('contraponto_user', JSON.stringify(data.user));
        localStorage.setItem('contraponto_token', data.token);
        if (data.refreshToken) {
            localStorage.setItem('contraponto_refresh_token', data.refreshToken);
        }
        
        // Store token expiry (default: 24 hours from now)
        const expiryTime = Date.now() + (24 * 60 * 60 * 1000);
        localStorage.setItem('contraponto_token_expiry', expiryTime.toString());

        this.currentUser = data.user;
        this.isAuthenticated = true;
    }

    async refreshToken() {
        // Prevent multiple simultaneous refresh requests
        if (this.refreshPromise) {
            return this.refreshPromise;
        }

        this.refreshPromise = (async () => {
            const refreshToken = localStorage.getItem('contraponto_refresh_token');
            if (!refreshToken) {
                this.refreshPromise = null;
                return false;
            }

            try {
                const response = await fetch(`${this.apiUrl}/refresh`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ refreshToken: refreshToken })
                });

                const data = await response.json();

                if (response.ok) {
                    this.storeSession(data);
                    this.refreshPromise = null;
                    return true;
                } else {
                    this.logout();
                    this.refreshPromise = null;
                    return false;
                }
            } catch (error) {
                console.error('Token refresh error:', error);
                this.refreshPromise = null;
                return false;
            }
        })();

        return this.refreshPromise;
    }

    async authenticatedFetch(url, options = {}) {
        let token = localStorage.getItem('contraponto_token');
        
        if (!token) {
            throw new Error('No authentication token');
        }

        const executeFetch = async (requestToken, isRetry = false) => {
            const response = await fetch(url, {
                ...options,
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers,
                    'Authorization': `Bearer ${requestToken}`
                }
            });

            // If unauthorized and not already a retry, try to refresh
            if (response.status === 401 && !isRetry) {
                const refreshed = await this.refreshToken();
                if (refreshed) {
                    const newToken = localStorage.getItem('contraponto_token');
                    return executeFetch(newToken, true);
                }
            }
            return response;
        };

        return executeFetch(token);
    }

    // Helper method for GET requests
    async get(url) {
        return this.authenticatedFetch(url, { method: 'GET' });
    }

    // Helper method for POST requests
    async post(url, body) {
        return this.authenticatedFetch(url, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    }

    // Helper method for PUT requests
    async put(url, body) {
        return this.authenticatedFetch(url, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    }

    // Helper method for DELETE requests
    async delete(url) {
        return this.authenticatedFetch(url, { method: 'DELETE' });
    }

    async logout() {
        // Call logout endpoint (don't await, fire and forget)
        const token = localStorage.getItem('contraponto_token');
        if (token) {
            fetch(`${this.apiUrl}/logout`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            }).catch(err => console.error('Logout error:', err));
        }

        // Clear local storage
        localStorage.removeItem('contraponto_user');
        localStorage.removeItem('contraponto_token');
        localStorage.removeItem('contraponto_refresh_token');
        localStorage.removeItem('contraponto_token_expiry');

        this.currentUser = null;
        this.isAuthenticated = false;
        this.renderAuthUI();
        
        // Only reload if not on public page
        const publicPaths = ['/', '/post/'];
        const isPublicPath = publicPaths.some(path => window.location.pathname === path || window.location.pathname.startsWith('/post/'));
        
        if (!isPublicPath) {
            window.location.href = '/';
        } else {
            window.location.reload();
        }
    }

    renderAuthUI() {
        const authSection = document.getElementById('authSection');
        if (!authSection) return;

        if (this.isAuthenticated && this.currentUser) {
            const avatarInitial = this.currentUser.name.charAt(0).toUpperCase();
            const firstName = this.currentUser.name.split(' ')[0];
            const avatarUrl = this.currentUser.avatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(this.currentUser.name)}&background=1a8917&color=fff&bold=true`;

            authSection.innerHTML = `
                <div class="user-menu">
                    <button class="user-menu__button" id="userMenuBtn" aria-label="User menu">
                        <img src="${avatarUrl}" alt="${this.currentUser.name}" class="user-menu__avatar" onerror="this.src='https://ui-avatars.com/api/?name=${avatarInitial}&background=1a8917&color=fff'"/>
                        <span>${firstName}</span>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                            <path d="M6 9L12 15L18 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                        </svg>
                    </button>
                    <div class="user-menu__dropdown" id="userDropdown">
                        <div class="user-menu__header">
                            <div class="user-menu__name">${this.escapeHtml(this.currentUser.name)}</div>
                            <div class="user-menu__email">${this.escapeHtml(this.currentUser.email)}</div>
                        </div>
                        <div class="user-menu__items">
                            <a href="/profile" class="user-menu__item" data-nav>
                                <svg viewBox="0 0 24 24" fill="none" width="18" height="18">
                                    <path d="M20 21V19C20 16.8 18.2 15 16 15H8C5.8 15 4 16.8 4 19V21M16 7C16 9.2 14.2 11 12 11C9.8 11 8 9.2 8 7C8 4.8 9.8 3 12 3C14.2 3 16 4.8 16 7Z" stroke="currentColor" stroke-width="1.5"/>
                                </svg>
                                Profile
                            </a>
                            <a href="/write" class="user-menu__item" data-nav>
                                <svg viewBox="0 0 24 24" fill="none" width="18" height="18">
                                    <path d="M12 4V20M4 12H20" stroke="currentColor" stroke-width="1.5"/>
                                </svg>
                                Write
                            </a>
                            <a href="/dashboard" class="user-menu__item" data-nav>
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

            this.setupUserMenu();
        } else {
            authSection.innerHTML = `
                <div class="auth-buttons">
                    <button class="auth-btn auth-btn-signup" id="signupBtn">Sign Up</button>
                    <button class="auth-btn auth-btn-login" id="loginBtn">Sign In</button>
                </div>
            `;

            this.setupAuthButtons();
        }
    }

    setupUserMenu() {
        const userMenuBtn = document.getElementById('userMenuBtn');
        const userDropdown = document.getElementById('userDropdown');
        const logoutBtn = document.getElementById('logoutBtn');

        if (userMenuBtn && userDropdown) {
            const toggleDropdown = (e) => {
                e.stopPropagation();
                userDropdown.classList.toggle('user-menu__dropdown--open');
            };
            
            userMenuBtn.addEventListener('click', toggleDropdown);

            // Close dropdown when clicking outside
            const closeDropdown = (e) => {
                if (!userMenuBtn.contains(e.target) && !userDropdown.contains(e.target)) {
                    userDropdown.classList.remove('user-menu__dropdown--open');
                }
            };
            document.addEventListener('click', closeDropdown);

            // Close on escape key
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && userDropdown.classList.contains('user-menu__dropdown--open')) {
                    userDropdown.classList.remove('user-menu__dropdown--open');
                }
            });
        }

        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.logout());
        }

        // Handle navigation links
        document.querySelectorAll('[data-nav]').forEach(link => {
            link.addEventListener('click', (e) => {
                const href = link.getAttribute('href');
                if (href && !href.startsWith('http')) {
                    e.preventDefault();
                    // Close dropdown before navigation
                    if (userDropdown) {
                        userDropdown.classList.remove('user-menu__dropdown--open');
                    }
                    window.location.href = href;
                }
            });
        });
    }

    setupAuthButtons() {
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

    escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
}

// Initialize auth on page load
const authManager = new AuthManager();

// Make auth manager globally available
window.authManager = authManager;