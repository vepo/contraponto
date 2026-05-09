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
        // Single delegated click handler for both opening and closing
        document.body.addEventListener('click', (evt) => {
            const userMenuBtn = document.getElementById('userMenuBtn');
            const userDropdown = document.getElementById('userDropdown');
            if (!userMenuBtn || !userDropdown) return;

            const isButtonClick = userMenuBtn.contains(evt.target);
            const isInsideDropdown = userDropdown.contains(evt.target);

            if (isButtonClick) {
                evt.stopPropagation();
                userDropdown.classList.toggle('user-menu__dropdown--open');
            } else if (!isInsideDropdown || evt.target.dataset.hxGet) {
                userDropdown.classList.remove('user-menu__dropdown--open');
            }
        });

        document.addEventListener('keydown', (evt) => {
            const userDropdown = document.getElementById('userDropdown');
            if (evt.key === 'Escape' && userDropdown?.classList.contains('user-menu__dropdown--open')) {
                userDropdown.classList.remove('user-menu__dropdown--open');
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

        // Close on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && sidebar.classList.contains('open')) {
                closeSidebar();
            }
        });

        // Close sidebar after HTMX navigation (when main content updates)
        document.body.addEventListener('htmx:afterSwap', (evt) => {
            const target = evt.detail.target;
            if (target && (target.id === 'main' || target.tagName === 'MAIN' || target.closest('main'))) {
                closeSidebar();
            }
        });

        // Close sidebar when any sidebar link/button is clicked
        const sidebarLinks = sidebar.querySelectorAll('[data-hx-get], .sidebar__link--button');
        sidebarLinks.forEach(link => {
            link.addEventListener('click', () => {
                setTimeout(closeSidebar, 150); // allow HTMX request to start
            });
        });
    }

    setupHtmxListener() {
        // After any HTMX swap that replaces the menu container, ensure the dropdown is closed
        document.body.addEventListener('htmx:afterSwap', (evt) => {
            if (evt && evt.detail && evt.detail.target) {
                const menuContainer = document.getElementById('menu-container');
                if (menuContainer && evt.detail.target.contains(menuContainer)) {
                    const userDropdown = document.getElementById('userDropdown');
                    if (userDropdown) {
                        userDropdown.classList.remove('user-menu__dropdown--open');
                    }
                }
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new HeaderManager();
});