class MainManager {
    constructor() {
        this.protectedPaths = ['/write'];
        // Bind methods to this instance
        this.redirectIfPathProtected = this.redirectIfPathProtected.bind(this);
        this.disabledElementsBasedOnUrl = this.disabledElementsBasedOnUrl.bind(this);
        this.setupRouteBasedElementsEnabler();
    }

    /**
     * Existing logic: enable/disable elements based on URL pattern.
     */
    setupRouteBasedElementsEnabler() {
        htmx.on('loggedOut', this.redirectIfPathProtected);
        document.body.addEventListener('htmx:afterSwap', this.disabledElementsBasedOnUrl);
    }

    disabledElementsBasedOnUrl() {
        document.querySelectorAll("[data-disable-pattern]")
            .forEach(elm => {
                const pattern = new RegExp(elm.dataset.disablePattern);
                if (pattern.test(window.location.pathname)) {
                    elm.classList.add('disabled');
                } else {
                    elm.classList.remove('disabled');
                }
            })
    }

    /**
     * Redirects to '/' if the current path is protected and not already '/'.
     * Ignores query parameters and fragments.
     */
    redirectIfPathProtected() {
        const currentPath = window.location.pathname;
        if (currentPath !== '/' && this.isPathProtected(currentPath)) {
            console.log(`Protected path detected: ${currentPath}, redirecting to /`);
            window.location.href = '/';
        }
    }

    /**
     * Checks if the given pathname is in the protected list.
     */
    isPathProtected(pathname) {
        return this.protectedPaths.includes(pathname);
    }
}
document.addEventListener('DOMContentLoaded', () => {
    new MainManager();
});