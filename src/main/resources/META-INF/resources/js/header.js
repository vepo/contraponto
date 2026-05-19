class HeaderManager {
    constructor() {
        this.init();
        this.setupHtmxListener();
        this.setupSidebar();
    }

    init() {
        this.setupUserMenu();
    }

    setupUserMenu() {
        this.bindUserMenuToggle = () => {
            const userMenuBtn = document.getElementById('userMenuBtn');
            const userDropdown = document.getElementById('userDropdown');
            if (!userMenuBtn || !userDropdown || userMenuBtn.dataset.menuBound) {
                return;
            }
            userMenuBtn.dataset.menuBound = 'true';
            userMenuBtn.addEventListener('click', (evt) => {
                evt.preventDefault();
                evt.stopPropagation();
                userDropdown.classList.toggle('user-menu__dropdown--open');
            });
        };
        this.bindUserMenuToggle();

        if (!this.userMenuOutsideClickBound) {
            this.userMenuOutsideClickBound = true;
            document.body.addEventListener('click', (evt) => {
                const userMenuBtn = document.getElementById('userMenuBtn');
                const userDropdown = document.getElementById('userDropdown');
                if (!userMenuBtn || !userDropdown) {
                    return;
                }
                if (userMenuBtn.contains(evt.target)) {
                    return;
                }
                if (userDropdown.contains(evt.target)) {
                    if (evt.target.closest('[data-hx-get]')) {
                        userDropdown.classList.remove('user-menu__dropdown--open');
                    }
                    return;
                }
                userDropdown.classList.remove('user-menu__dropdown--open');
            });

            document.addEventListener('keydown', (evt) => {
                const userDropdown = document.getElementById('userDropdown');
                if (evt.key === 'Escape' && userDropdown?.classList.contains('user-menu__dropdown--open')) {
                    userDropdown.classList.remove('user-menu__dropdown--open');
                }
            });
        }
    }

    rebindUserMenu() {
        const userMenuBtn = document.getElementById('userMenuBtn');
        const userDropdown = document.getElementById('userDropdown');
        if (userMenuBtn) {
            delete userMenuBtn.dataset.menuBound;
        }
        if (userDropdown) {
            userDropdown.classList.remove('user-menu__dropdown--open');
        }
        this.bindUserMenuToggle?.();
    }

    isMenuContainerSwap(target) {
        if (!target || target.nodeType !== Node.ELEMENT_NODE) {
            return false;
        }
        if (target.id === 'menu-container') {
            return true;
        }
        return target.closest?.('#menu-container') != null;
    }

    setupHtmxListener() {
        const onPossibleMenuSwap = (evt) => {
            if (this.isMenuContainerSwap(evt.detail?.target)) {
                this.rebindUserMenu();
            }
        };

        document.body.addEventListener('htmx:afterSwap', onPossibleMenuSwap);
        document.body.addEventListener('htmx:oobAfterSwap', onPossibleMenuSwap);

        // Auth forms fire loggedIn on body after menu OOB swap (see HtmxTriggers.LOGGED_IN_ON_BODY)
        document.body.addEventListener('loggedIn', () => this.rebindUserMenu());

        // Fallback if swap events were missed
        document.body.addEventListener('htmx:afterSettle', () => {
            const userMenuBtn = document.getElementById('userMenuBtn');
            if (userMenuBtn && !userMenuBtn.dataset.menuBound) {
                this.rebindUserMenu();
            }
        });
    }

    setupSidebar() {
        const menuBtn = document.getElementById('menuBtn');
        const sidebar = document.getElementById('sidebar');
        const overlay = document.getElementById('sidebarOverlay');
        const closeBtn = sidebar ? sidebar.querySelector('.sidebar__close') : null;

        if (!menuBtn || !sidebar || !overlay) return;

        const openSidebar = () => {
            sidebar.classList.add('open');
            overlay.classList.add('active');
            document.body.classList.add('sidebar-open');
        };

        const closeSidebar = () => {
            sidebar.classList.remove('open');
            overlay.classList.remove('active');
            document.body.classList.remove('sidebar-open');
        };

        menuBtn.addEventListener('click', openSidebar);
        if (closeBtn) closeBtn.addEventListener('click', closeSidebar);
        overlay.addEventListener('click', closeSidebar);

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && sidebar.classList.contains('open')) {
                closeSidebar();
            }
        });

        document.body.addEventListener('htmx:afterSwap', (evt) => {
            const target = evt.detail.target;
            if (target && (target.id === 'main' || target.tagName === 'MAIN' || target.closest('main'))) {
                closeSidebar();
            }
        });

        const sidebarLinks = sidebar.querySelectorAll('[data-hx-get], .sidebar__link--button');
        sidebarLinks.forEach(link => {
            link.addEventListener('click', () => {
                setTimeout(closeSidebar, 150);
            });
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new HeaderManager();
});
