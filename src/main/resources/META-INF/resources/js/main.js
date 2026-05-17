class MainManager {
    constructor() {
        this.protectedPaths = ['/write', '/profile', '/blogs', '/pages', '/users', '/write/draft/[0-9]+'];
        // Bind methods to this instance
        this.redirectIfPathProtected = this.redirectIfPathProtected.bind(this);
        this.updateUIElements = this.updateUIElements.bind(this);
        this.bindErrorMessage = this.bindErrorMessage.bind(this);
        this.setupRouteBasedElementsEnabler();
        this.setupErrorHandler();
        this.updateUIElements();
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
                errorMessages.forEach(elm => elm.classList.add('visible'));
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
        document.body.addEventListener('htmx:afterSettle', this.updateUIElements);
    }

    updateUIElements(evt) {
        if (evt && evt.detail && evt.detail.target && evt.detail.target.id === 'libraryContent') {
            const requestPath = evt.detail.pathInfo?.requestPath || '';
            let newActiveValue = null;
            if (requestPath) {
                try {
                    const url = new URL(requestPath, window.location.origin);
                    newActiveValue = url.searchParams.get('tab') || url.searchParams.get('type');
                } catch (e) {
                    /* ignore malformed path */
                }
                if (!newActiveValue) {
                    const m = requestPath.match(/\/library\/components\/tab\/(drafts|published)(?:\?|$)/);
                    if (m) newActiveValue = m[1];
                }
            }
            if (newActiveValue) {
                const activeTab = document.querySelector('.library-tab--active');
                if (activeTab && activeTab.dataset.tab !== newActiveValue) {
                    activeTab.classList.remove('library-tab--active');
                    const newActive = document.querySelector(`.library-tab[data-tab="${newActiveValue}"]`);
                    if (newActive) newActive.classList.add('library-tab--active');
                }
            }
        }
        document.querySelectorAll("[data-disable-pattern]")
            .forEach(elm => {
                const pattern = new RegExp(elm.dataset.disablePattern);
                if (pattern.test(window.location.pathname)) {
                    elm.classList.add('disabled');
                } else {
                    elm.classList.remove('disabled');
                }
            });
        if (this.shouldHighlightCode(evt)) {
            const main = document.querySelector('main');
            if (main) {
                main.querySelectorAll('pre code').forEach(block => hljs.highlightElement(block));
            } else {
                hljs.highlightAll();
            }
        }
    }

    /**
     * Run syntax highlighting only when main content was swapped or on initial load.
     */
    shouldHighlightCode(evt) {
        if (!evt || !evt.detail?.target) {
            return true;
        }
        const target = evt.detail.target;
        if (target.id === 'main' || target.tagName === 'MAIN') {
            return true;
        }
        return target.closest?.('main') != null;
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

    closeModal(selector) {
        var modal = document.querySelector('.modal.modal--open');
        if (modal) {
            modal.classList.remove('modal--open');
        }

        if (selector) {
            var actionBtn = document.querySelector(selector);
            if (actionBtn) {
                actionBtn.click();
            }
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
    window.main = new MainManager();
});