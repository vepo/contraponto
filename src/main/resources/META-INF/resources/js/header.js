class HeaderManager {
    constructor() {
        this.init();
    }

    init() {
        this.setupUserMenu();
        // No htmx:xhr:loadend listener needed – delegation handles dynamic content
    }

    setupUserMenu() {
        // Single delegated click handler for both opening and closing
        document.addEventListener('click', (evt) => {
            const userMenuBtn = document.getElementById('userMenuBtn');
            const userDropdown = document.getElementById('userDropdown');
            if (!userMenuBtn || !userDropdown) return;

            const isButtonClick = userMenuBtn.contains(evt.target);
            const isInsideDropdown = userDropdown.contains(evt.target);

            if (isButtonClick) {
                evt.stopPropagation();
                userDropdown.classList.toggle('user-menu__dropdown--open');
            } else if (!isInsideDropdown) {
                userDropdown.classList.remove('user-menu__dropdown--open');
            }
        });

        document.body.addEventListener('htmx:xhr:loadend', evt => {
            if (evt && evt.target) {
                const sourceElm = evt.target;
                const userDropdown = document.getElementById('userDropdown');
                if (userDropdown.contains(sourceElm)) {
                    userDropdown.classList.remove('user-menu__dropdown--open');
                }
            }
        });

        // Single delegated keydown handler for Escape
        document.addEventListener('keydown', (evt) => {
            const userDropdown = document.getElementById('userDropdown');
            if (evt.key === 'Escape' && userDropdown?.classList.contains('user-menu__dropdown--open')) {
                userDropdown.classList.remove('user-menu__dropdown--open');
            }
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new HeaderManager();
});