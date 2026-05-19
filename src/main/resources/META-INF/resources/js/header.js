class HeaderManager {
    constructor() {
        this.init();
        this.setupHtmxListener();
        this.setupSidebar();
    }

    init() {
        this.setupUserMenu();
        this.setupNotificationMenu();
        this.setupLocalePicker();
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

    setupLocalePicker() {
        this.bindLocalePickerToggle = () => {
            const trigger = document.getElementById('localePickerBtn');
            const dropdown = document.getElementById('localePickerDropdown');
            if (!trigger || !dropdown || trigger.dataset.localePickerBound) {
                return;
            }
            trigger.dataset.localePickerBound = 'true';
            trigger.addEventListener('click', (evt) => {
                evt.preventDefault();
                evt.stopPropagation();
                const opening = !dropdown.classList.contains('locale-picker__dropdown--open');
                dropdown.classList.toggle('locale-picker__dropdown--open');
                trigger.setAttribute('aria-expanded', opening ? 'true' : 'false');
            });
        };
        this.bindLocalePickerToggle();

        if (!this.localePickerOutsideClickBound) {
            this.localePickerOutsideClickBound = true;
            document.body.addEventListener('click', (evt) => {
                const trigger = document.getElementById('localePickerBtn');
                const dropdown = document.getElementById('localePickerDropdown');
                if (!trigger || !dropdown) {
                    return;
                }
                if (trigger.contains(evt.target) || dropdown.contains(evt.target)) {
                    if (dropdown.contains(evt.target) && evt.target.closest('[data-locale]')) {
                        dropdown.classList.remove('locale-picker__dropdown--open');
                        trigger.setAttribute('aria-expanded', 'false');
                    }
                    return;
                }
                dropdown.classList.remove('locale-picker__dropdown--open');
                trigger.setAttribute('aria-expanded', 'false');
            });

            document.addEventListener('keydown', (evt) => {
                const dropdown = document.getElementById('localePickerDropdown');
                const trigger = document.getElementById('localePickerBtn');
                if (evt.key === 'Escape' && dropdown?.classList.contains('locale-picker__dropdown--open')) {
                    dropdown.classList.remove('locale-picker__dropdown--open');
                    if (trigger) {
                        trigger.setAttribute('aria-expanded', 'false');
                    }
                }
            });
        }
    }

    rebindLocalePicker() {
        const trigger = document.getElementById('localePickerBtn');
        const dropdown = document.getElementById('localePickerDropdown');
        if (trigger) {
            delete trigger.dataset.localePickerBound;
        }
        if (dropdown) {
            dropdown.classList.remove('locale-picker__dropdown--open');
        }
        this.bindLocalePickerToggle?.();
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

    loadNotificationOverlay(overlay) {
        if (!overlay || typeof htmx === 'undefined') {
            return;
        }
        overlay.dataset.loading = 'true';
        htmx.ajax('GET', '/components/notifications/overlay', {
            target: '#notificationOverlay',
            swap: 'innerHTML',
        });
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
                if (opening && !overlay.dataset.loaded && !overlay.dataset.loading) {
                    overlay.dataset.loaded = 'true';
                    this.loadNotificationOverlay(overlay);
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
        const wasOpen = overlay?.classList.contains('notification-menu__dropdown--open');
        if (bellBtn) {
            delete bellBtn.dataset.notificationBound;
        }
        if (overlay) {
            delete overlay.dataset.loaded;
            delete overlay.dataset.loading;
            overlay.classList.remove('notification-menu__dropdown--open');
        }
        this.bindNotificationMenuToggle?.();
        if (wasOpen && overlay) {
            overlay.classList.add('notification-menu__dropdown--open');
            if (bellBtn) {
                bellBtn.setAttribute('aria-expanded', 'true');
            }
            overlay.dataset.loaded = 'true';
            this.loadNotificationOverlay(overlay);
        }
    }

    isNotificationBadgeSwap(target) {
        if (!target || target.nodeType !== Node.ELEMENT_NODE) {
            return false;
        }
        return target.id === 'notification-badge-container';
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

        document.body.addEventListener('htmx:afterSwap', (evt) => {
            onPossibleMenuSwap(evt);
            const target = evt.detail?.target;
            if (target?.id === 'notificationOverlay') {
                delete target.dataset.loading;
            }
        });
        document.body.addEventListener('htmx:oobAfterSwap', onPossibleMenuSwap);

        document.body.addEventListener('loggedIn', () => {
            this.rebindUserMenu();
            this.rebindNotificationMenu();
        });

        document.body.addEventListener('notificationsChanged', () => {
            const overlay = document.getElementById('notificationOverlay');
            if (overlay?.classList.contains('notification-menu__dropdown--open')) {
                delete overlay.dataset.loaded;
                this.loadNotificationOverlay(overlay);
                overlay.dataset.loaded = 'true';
            }
        });

        document.body.addEventListener('htmx:afterSettle', () => {
            const userMenuBtn = document.getElementById('userMenuBtn');
            if (userMenuBtn && !userMenuBtn.dataset.menuBound) {
                this.rebindUserMenu();
            }
            const bellBtn = document.getElementById('notificationBellBtn');
            if (bellBtn && !bellBtn.dataset.notificationBound) {
                this.rebindNotificationMenu();
            }
            const localePickerBtn = document.getElementById('localePickerBtn');
            if (localePickerBtn && !localePickerBtn.dataset.localePickerBound) {
                this.rebindLocalePicker();
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
