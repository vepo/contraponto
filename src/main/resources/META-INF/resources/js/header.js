class HeaderManager {
    constructor() {
        this.init();
        this.setupHtmxListener();
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

    setupHtmxListener() {
        // After any HTMX swap that replaces the menu container, ensure the dropdown is closed
        document.body.addEventListener('htmx:afterSwap', (evt) => {
            console.log('swap on header', evt)
            const menuContainer = document.getElementById('menu-container');
            if (menuContainer && evt.detail.target.contains(menuContainer)) {
                const userDropdown = document.getElementById('userDropdown');
                if (userDropdown) {
                    userDropdown.classList.remove('user-menu__dropdown--open');
                }
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new HeaderManager();
});