class HeaderManager {
    constructor() {
        this.init();
        this.setupHtmxListener();
        this.setupSidebar();
    }

    init() {
        this.setupUserMenu();
        this.setupNotificationMenu();
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

    setupNotificationMenu() {
        this.bindNotificationMenuToggle = () => {
            const bellBtn = document.getElementById('notificationBellBtn');
            const overlay = document.getElementById('notificationOverlay');
            if (!bellBtn || !overlay || bellBtn.dataset.notificationBound) {
                return;
            }
            bellBtn.dataset.notificationBound = 'true';

            bellBtn.addEventListener('click', (evt) => {
                evt.preventDefault();
                evt.stopPropagation();
                const opening = !overlay.classList.contains('notification-menu__dropdown--open');
                overlay.classList.toggle('notification-menu__dropdown--open');
                bellBtn.setAttribute('aria-expanded', opening ? 'true' : 'false');
                if (opening && !overlay.dataset.loaded) {
                    overlay.dataset.loaded = 'true';
                    htmx.ajax('GET', '/components/notifications/overlay', {
                        target: '#notificationOverlay',
                        swap: 'innerHTML',
                    });
                }
            });

            overlay.addEventListener('click', (evt) => {
                const closeBtn = evt.target.closest('[data-notification-close]');
                if (closeBtn) {
                    evt.preventDefault();
                    this.closeNotificationOverlay();
                }
            });
        };
        this.bindNotificationMenuToggle();

        if (!this.notificationOutsideClickBound) {
            this.notificationOutsideClickBound = true;
            document.body.addEventListener('click', (evt) => {
                const bellBtn = document.getElementById('notificationBellBtn');
                const overlay = document.getElementById('notificationOverlay');
                if (!bellBtn || !overlay) {
                    return;
                }
                if (bellBtn.contains(evt.target)) {
                    return;
                }
                if (overlay.contains(evt.target)) {
                    if (evt.target.closest('[data-hx-get]')) {
                        this.closeNotificationOverlay();
                    }
                    return;
                }
                this.closeNotificationOverlay();
            });

            document.addEventListener('keydown', (evt) => {
                const overlay = document.getElementById('notificationOverlay');
                if (evt.key === 'Escape' && overlay?.classList.contains('notification-menu__dropdown--open')) {
                    this.closeNotificationOverlay();
                }
            });
        }
    }

    closeNotificationOverlay() {
        const bellBtn = document.getElementById('notificationBellBtn');
        const overlay = document.getElementById('notificationOverlay');
        if (overlay) {
            overlay.classList.remove('notification-menu__dropdown--open');
        }
        if (bellBtn) {
            bellBtn.setAttribute('aria-expanded', 'false');
        }
    }

    rebindNotificationMenu() {
        const bellBtn = document.getElementById('notificationBellBtn');
        const overlay = document.getElementById('notificationOverlay');
        if (bellBtn) {
            delete bellBtn.dataset.notificationBound;
        }
        if (overlay) {
            delete overlay.dataset.loaded;
            overlay.innerHTML = '';
            overlay.classList.remove('notification-menu__dropdown--open');
        }
        this.bindNotificationMenuToggle?.();
    }

    isNotificationBadgeSwap(target) {
        if (!target || target.nodeType !== Node.ELEMENT_NODE) {
            return false;
        }
        if (target.id === 'notification-badge-container') {
            return true;
        }
        return target.closest?.('#notification-badge-container') != null;
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
            if (this.isNotificationBadgeSwap(evt.detail?.target)) {
                this.rebindNotificationMenu();
            }
        };

        document.body.addEventListener('htmx:afterSwap', onPossibleMenuSwap);
        document.body.addEventListener('htmx:oobAfterSwap', onPossibleMenuSwap);

        // Auth forms fire loggedIn on body after menu OOB swap (see HtmxTriggers.LOGGED_IN_ON_BODY)
        document.body.addEventListener('loggedIn', () => {
            this.rebindUserMenu();
            this.rebindNotificationMenu();
        });

        document.body.addEventListener('notificationsChanged', () => {
            const overlay = document.getElementById('notificationOverlay');
            if (overlay?.classList.contains('notification-menu__dropdown--open')) {
                htmx.ajax('GET', '/components/notifications/overlay', {
                    target: '#notificationOverlay',
                    swap: 'innerHTML',
                });
            }
        });

        // Fallback if swap events were missed
        document.body.addEventListener('htmx:afterSettle', () => {
            const userMenuBtn = document.getElementById('userMenuBtn');
            if (userMenuBtn && !userMenuBtn.dataset.menuBound) {
                this.rebindUserMenu();
            }
            const bellBtn = document.getElementById('notificationBellBtn');
            if (bellBtn && !bellBtn.dataset.notificationBound) {
                this.rebindNotificationMenu();
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
