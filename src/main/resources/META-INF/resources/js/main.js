class MainManager {
    constructor() {
        this.protectedPaths = ['/write', '/profile'];
        // Bind methods to this instance
        this.redirectIfPathProtected = this.redirectIfPathProtected.bind(this);
        this.disabledElementsBasedOnUrl = this.disabledElementsBasedOnUrl.bind(this);
        this.bindErrorMessage = this.bindErrorMessage.bind(this);
        this.setupRouteBasedElementsEnabler();
        this.setupErrorHandler();
        this.disabledElementsBasedOnUrl();
    }

    setupErrorHandler() {
        htmx.on('htmx:afterRequest', this.bindErrorMessage);
    }

    bindErrorMessage(evt) {
        const errorElmSelector = evt.target.attributes.getNamedItem('hx-target-error')
        if (errorElmSelector) {
            if (evt.detail.failed) {
                let msg = "Something bad happened. Please contact site admin";
                if (evt.detail.xhr.responseText) {
                    msg = evt.detail.xhr.responseText;
                }

                const errorElm = document.querySelector(errorElmSelector.value);
                errorElm.innerHTML = msg;
                const errorMessages = errorElm.querySelectorAll('.error-message');
                errorMessages.forEach(elm => elm.style.display = 'block');
            } else {
                const errorElm = document.querySelector(errorElmSelector.value);
                while (errorElm.firstChild) {
                    errorElm.removeChild(errorElm.firstChild);
                }
            }
        }
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