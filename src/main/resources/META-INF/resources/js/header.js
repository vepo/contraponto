class HeaderManager {
    constructor() {
        this.searchModal = null;
        this.userDropdown = null;
        this.init();
    }

    init() {
        this.setupMenuButton();
        this.setupSearchButton();
        this.setupUserMenu();
        this.setupAuthButtons();
        this.setupWriteButton();
        this.setupLogoutButton();
    }

    setupMenuButton() {
        const menuBtn = document.getElementById('menuBtn');
        if (menuBtn) {
            menuBtn.addEventListener('click', () => {
                // Toggle sidebar/mobile menu
                const sidebar = document.getElementById('sidebar');
                if (sidebar) {
                    sidebar.classList.toggle('sidebar--open');
                } else {
                    // Create and show mobile menu
                    this.showMobileMenu();
                }
            });
        }
    }

    showMobileMenu() {
        // Create mobile menu overlay if it doesn't exist
        let mobileMenu = document.getElementById('mobileMenu');
        if (!mobileMenu) {
            mobileMenu = document.createElement('div');
            mobileMenu.id = 'mobileMenu';
            mobileMenu.className = 'mobile-menu';
            
            // Clone navigation links from header
            const navRight = document.querySelector('.nav-right');
            if (navRight) {
                const clone = navRight.cloneNode(true);
                mobileMenu.appendChild(clone);
            }
            
            document.body.appendChild(mobileMenu);
            
            // Close button
            const closeBtn = document.createElement('button');
            closeBtn.className = 'mobile-menu__close';
            closeBtn.innerHTML = '×';
            closeBtn.addEventListener('click', () => {
                mobileMenu.classList.remove('mobile-menu--open');
            });
            mobileMenu.insertBefore(closeBtn, mobileMenu.firstChild);
        }
        
        mobileMenu.classList.add('mobile-menu--open');
    }

    setupSearchButton() {
        const searchBtn = document.getElementById('searchBtn');
        const searchModal = document.getElementById('searchModal');
        const closeSearchBtn = document.getElementById('closeSearchBtn');
        const searchInput = document.getElementById('searchInput');

        if (searchBtn && searchModal) {
            searchBtn.addEventListener('click', () => {
                searchModal.classList.add('search-modal--open');
                if (searchInput) {
                    setTimeout(() => searchInput.focus(), 100);
                }
            });
        }

        if (closeSearchBtn && searchModal) {
            closeSearchBtn.addEventListener('click', () => {
                searchModal.classList.remove('search-modal--open');
                this.clearSearchResults();
            });
        }

        if (searchModal) {
            searchModal.addEventListener('click', (e) => {
                if (e.target === searchModal) {
                    searchModal.classList.remove('search-modal--open');
                    this.clearSearchResults();
                }
            });
        }

        if (searchInput) {
            let debounceTimer;
            searchInput.addEventListener('input', (e) => {
                clearTimeout(debounceTimer);
                const query = e.target.value.trim();
                
                if (query.length >= 2) {
                    debounceTimer = setTimeout(() => this.performSearch(query), 300);
                } else if (query.length === 0) {
                    this.clearSearchResults();
                }
            });

            searchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Escape') {
                    searchModal.classList.remove('search-modal--open');
                    this.clearSearchResults();
                }
            });
        }
    }

    async performSearch(query) {
        const resultsContainer = document.getElementById('searchResults');
        if (!resultsContainer) return;

        // Show loading state
        resultsContainer.innerHTML = '<div class="search-modal__loading">Searching...</div>';

        try {
            const response = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
            if (response.ok) {
                const results = await response.json();
                this.displaySearchResults(results);
            } else {
                resultsContainer.innerHTML = '<div class="search-modal__error">Failed to search. Please try again.</div>';
            }
        } catch (error) {
            console.error('Search error:', error);
            resultsContainer.innerHTML = '<div class="search-modal__error">An error occurred. Please try again.</div>';
        }
    }

    displaySearchResults(results) {
        const resultsContainer = document.getElementById('searchResults');
        if (!resultsContainer) return;

        if (!results || results.length === 0) {
            resultsContainer.innerHTML = '<div class="search-modal__empty">No results found.</div>';
            return;
        }

        const html = `
            <div class="search-results">
                ${results.map(post => `
                    <a href="/post/${post.slug}" class="search-result">
                        ${post.cover ? `
                            <div class="search-result__image">
                                <img src="${post.cover.url}" alt="${post.title}">
                            </div>
                        ` : ''}
                        <div class="search-result__content">
                            <h3 class="search-result__title">${this.escapeHtml(post.title)}</h3>
                            <p class="search-result__excerpt">${this.escapeHtml(post.description || post.content?.substring(0, 150) || '')}</p>
                            <div class="search-result__meta">
                                <span>${this.escapeHtml(post.author)}</span>
                                <span>•</span>
                                <span>${post.publishedAt ? new Date(post.publishedAt).toLocaleDateString() : ''}</span>
                            </div>
                        </div>
                    </a>
                `).join('')}
            </div>
        `;
        resultsContainer.innerHTML = html;
    }

    clearSearchResults() {
        const resultsContainer = document.getElementById('searchResults');
        if (resultsContainer) {
            resultsContainer.innerHTML = '';
        }
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = '';
        }
    }

    setupUserMenu() {
        const userMenuBtn = document.getElementById('userMenuBtn');
        const userDropdown = document.getElementById('userDropdown');

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

            // Close on escape key
            document.addEventListener('keydown', (e) => {
                if (e.key === 'Escape' && userDropdown.classList.contains('user-menu__dropdown--open')) {
                    userDropdown.classList.remove('user-menu__dropdown--open');
                }
            });
        }
    }

    setupAuthButtons() {
        const signupBtn = document.getElementById('signupBtn');
        const loginBtn = document.getElementById('loginBtn');

        if (signupBtn && window.authManager) {
            signupBtn.addEventListener('click', () => {
                window.authManager.isSignupMode = true;
                window.authManager.updateModalForMode();
                window.authManager.openModal();
            });
        }

        if (loginBtn && window.authManager) {
            loginBtn.addEventListener('click', () => {
                window.authManager.isSignupMode = false;
                window.authManager.updateModalForMode();
                window.authManager.openModal();
            });
        }
    }

    setupWriteButton() {
        const writeBtn = document.getElementById('writeBtn');
        if (writeBtn) {
            writeBtn.addEventListener('click', () => {
                window.location.href = '/write';
            });
        }
    }

    setupLogoutButton() {
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn && window.authManager) {
            logoutBtn.addEventListener('click', () => {
                window.authManager.logout();
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

// Initialize header manager when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.headerManager = new HeaderManager();
});