class ImageLightboxManager {
    constructor() {
        this.lightbox = document.getElementById('image-lightbox');
        if (!this.lightbox) {
            return;
        }
        this.imageEl = this.lightbox.querySelector('.image-lightbox__image');
        this.captionEl = this.lightbox.querySelector('.image-lightbox__caption');
        this.closeBtn = this.lightbox.querySelector('.image-lightbox__close');
        this.lastTrigger = null;
        this.onDocumentClick = this.onDocumentClick.bind(this);
        this.onKeyDown = this.onKeyDown.bind(this);
        document.addEventListener('click', this.onDocumentClick);
        this.closeBtn.addEventListener('click', () => this.close());
        this.lightbox.querySelectorAll('[data-lightbox-close]').forEach((el) => {
            el.addEventListener('click', () => this.close());
        });
    }

    onDocumentClick(event) {
        if (!this.lightbox || this.lightbox.hidden) {
            const trigger = event.target.closest('article.article-page__content img');
            if (!trigger || trigger.closest('.content-render')) {
                return;
            }
            event.preventDefault();
            event.stopPropagation();
            this.open(trigger);
            return;
        }
        if (this.lightbox.contains(event.target)) {
            return;
        }
    }

    static isLikelyFilename(caption, src) {
        if (!caption) {
            return false;
        }
        try {
            const urlName = decodeURIComponent(new URL(src, window.location.origin).pathname.split('/').pop() || '');
            if (caption === urlName) {
                return true;
            }
            const base = urlName.replace(/\.[^.]+$/, '');
            if (caption === base) {
                return true;
            }
        } catch (_) {
            /* ignore malformed URL */
        }
        return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\.[a-z0-9]+)?$/i.test(caption);
    }

    resolveCaption(trigger, src) {
        const blockTitle = trigger.closest('.imageblock')?.querySelector('.title')?.textContent?.trim();
        let caption = blockTitle || (trigger.alt || '').trim();
        if (!blockTitle && caption && ImageLightboxManager.isLikelyFilename(caption, src)) {
            caption = '';
        }
        return caption;
    }

    open(trigger) {
        const src = trigger.currentSrc || trigger.src;
        if (!src) {
            return;
        }
        this.lastTrigger = trigger;
        this.imageEl.src = src;
        this.imageEl.alt = trigger.alt || '';
        const caption = this.resolveCaption(trigger, src);
        this.captionEl.textContent = caption;
        this.lightbox.hidden = false;
        this.lightbox.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';
        document.addEventListener('keydown', this.onKeyDown);
        this.closeBtn.focus();
    }

    close() {
        if (!this.lightbox || this.lightbox.hidden) {
            return;
        }
        this.lightbox.hidden = true;
        this.lightbox.setAttribute('aria-hidden', 'true');
        this.imageEl.removeAttribute('src');
        document.body.style.overflow = '';
        document.removeEventListener('keydown', this.onKeyDown);
        if (this.lastTrigger) {
            this.lastTrigger.focus();
            this.lastTrigger = null;
        }
    }

    onKeyDown(event) {
        if (event.key === 'Escape') {
            event.preventDefault();
            this.close();
        }
    }
}

class CodeCopyManager {
    static COPY_KEY = 'post.codeBlock.copy';
    static COPIED_KEY = 'post.codeBlock.copied';
    static ENHANCED_ATTR = 'data-code-copy-enhanced';

    static copyLabel() {
        const translated = window.i18n?.t(CodeCopyManager.COPY_KEY);
        return translated && translated !== CodeCopyManager.COPY_KEY ? translated : 'Copiar';
    }

    static copiedLabel() {
        const translated = window.i18n?.t(CodeCopyManager.COPIED_KEY);
        return translated && translated !== CodeCopyManager.COPIED_KEY ? translated : 'Copiado';
    }

    constructor() {
        this.scopes = ['article.article-page__content', '.write-preview'];
    }

    enhanceAll() {
        for (const scope of this.scopes) {
            document.querySelectorAll(scope).forEach((root) => this.enhance(root));
        }
    }

    enhance(root) {
        root.querySelectorAll('pre code').forEach((code) => {
            const pre = code.closest('pre');
            if (!pre || pre.closest('.verseblock')) {
                return;
            }
            const container = this.containerFor(pre);
            if (!container || container.getAttribute(CodeCopyManager.ENHANCED_ATTR)) {
                return;
            }
            container.setAttribute(CodeCopyManager.ENHANCED_ATTR, 'true');
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'code-block__copy';
            button.setAttribute('aria-label', CodeCopyManager.copyLabel());
            button.textContent = CodeCopyManager.copyLabel();
            button.dataset.i18n = CodeCopyManager.COPY_KEY;
            button.dataset.i18nAttr = 'aria-label';
            button.addEventListener('click', (event) => {
                event.preventDefault();
                this.copy(code, button);
            });
            container.insertBefore(button, container.firstChild);
        });
    }

    containerFor(pre) {
        const listingblock = pre.closest('.listingblock');
        if (listingblock) {
            return listingblock;
        }
        const existing = pre.parentElement;
        if (existing?.classList.contains('code-block')) {
            return existing;
        }
        const wrapper = document.createElement('div');
        wrapper.className = 'code-block';
        pre.parentNode.insertBefore(wrapper, pre);
        wrapper.appendChild(pre);
        return wrapper;
    }

    copy(code, button) {
        const text = code.innerText;
        if (!text) {
            return;
        }
        this.writeClipboard(text);
        button.textContent = CodeCopyManager.copiedLabel();
        button.classList.add('code-block__copy--copied');
        button.setAttribute('aria-label', CodeCopyManager.copiedLabel());
        window.clearTimeout(button._copyResetTimer);
        button._copyResetTimer = window.setTimeout(() => {
            button.textContent = CodeCopyManager.copyLabel();
            button.classList.remove('code-block__copy--copied');
            button.setAttribute('aria-label', CodeCopyManager.copyLabel());
        }, 2000);
    }

    writeClipboard(text) {
        if (navigator.clipboard?.writeText) {
            navigator.clipboard.writeText(text).catch(() => this.fallbackCopy(text));
            return;
        }
        this.fallbackCopy(text);
    }

    fallbackCopy(text) {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.setAttribute('readonly', '');
        textarea.style.position = 'fixed';
        textarea.style.left = '-9999px';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
    }
}

class MainManager {
    constructor() {
        this.protectedPaths = [
            '/write',
            '/account/security',
            '/manage/dashboard',
            '/writing/library',
            '/writing/blogs',
            '/writing/appearance',
            '/manage/pages',
            '/manage/comments',
            '/administration/users',
            '/editor/review',
            '/editor/tags',
            '/account/notifications',
            '/account/subscriptions',
            '/blogs',
            '/write/draft/[0-9]+'
        ];
        // Bind methods to this instance
        this.redirectIfPathProtected = this.redirectIfPathProtected.bind(this);
        this.updateUIElements = this.updateUIElements.bind(this);
        this.bindErrorMessage = this.bindErrorMessage.bind(this);
        this.handleSearchModalSubmit = this.handleSearchModalSubmit.bind(this);
        this.setupRouteBasedElementsEnabler();
        this.setupErrorHandler();
        this.setupCsrfHeader();
        this.setupMainNavigationSwap();
        this.setupMainNavigationScroll();
        this.setupModifierKeyNavigation();
        this.setupSeoSync();
        this.setupSearchModalSubmit();
        this.updateUIElements();
    }

    /**
     * Full-page HTMX nav selects <main> from the response and swaps the current <main>.
     * Default swap (innerHTML) nests <main> inside <main>; outerHTML replaces the element.
     */
    setupMainNavigationSwap() {
        const applyOuterHtmlSwap = (elt) => {
            if (!elt || elt.getAttribute('hx-target') !== 'main' || elt.getAttribute('hx-select') !== 'main') {
                return;
            }
            const swap = elt.getAttribute('hx-swap');
            if (!swap || swap === 'innerHTML') {
                elt.setAttribute('hx-swap', 'outerHTML');
            }
        };

        const patchMainNavigationSwaps = (root = document) => {
            root.querySelectorAll('[hx-target="main"][hx-select="main"]').forEach(applyOuterHtmlSwap);
        };

        document.body.addEventListener('htmx:beforeProcessNode', (evt) => {
            applyOuterHtmlSwap(evt.detail.elt);
        });

        document.body.addEventListener('htmx:afterSettle', () => {
            this.repairNestedMain();
            patchMainNavigationSwaps();
            document.body.style.removeProperty('min-height');
            document.body.style.removeProperty('height');
            document.documentElement.style.removeProperty('min-height');
            document.documentElement.style.removeProperty('height');
        });

        patchMainNavigationSwaps();
    }

    /**
     * HTMX "show:window:top" resolves "body" inside the swapped <main>, not the document —
     * so scroll position is preserved on SPA navigation. Reset the window after main swaps.
     */
    setupMainNavigationScroll() {
        let historyRestorePending = false;

        document.body.addEventListener('htmx:historyRestore', () => {
            historyRestorePending = true;
        });

        document.body.addEventListener('htmx:afterSettle', (evt) => {
            if (historyRestorePending) {
                historyRestorePending = false;
                return;
            }
            if (!MainManager.isMainNavigationSwap(evt)) {
                return;
            }
            const path = evt.detail?.requestConfig?.path || evt.detail?.pathInfo?.finalRequestPath || '';
            const anchor = evt.detail?.pathInfo?.anchor;
            if (anchor || path.includes('#')) {
                return;
            }
            window.setTimeout(() => {
                window.scrollTo(0, 0);
            }, 0);
        });
    }

    static isMainNavigationSwap(evt) {
        const swapTarget = evt.detail?.target;
        if (!swapTarget || swapTarget.tagName !== 'MAIN') {
            return false;
        }
        const trigger = evt.detail?.requestConfig?.elt;
        if (!trigger) {
            return false;
        }
        return trigger.getAttribute('hx-target') === 'main'
            && trigger.getAttribute('hx-select') === 'main';
    }

    /**
     * Ctrl/Cmd/Shift/middle-click on SPA nav anchors should open href in a new tab,
     * not run HTMX in the current tab (HTMX only skips modifier clicks for hx-boost).
     */
    setupModifierKeyNavigation() {
        document.body.addEventListener('htmx:configRequest', (evt) => {
            const triggering = evt.detail?.triggeringEvent;
            if (!triggering || (triggering.type !== 'click' && triggering.type !== 'auxclick')) {
                return;
            }
            const modifierClick = triggering.ctrlKey
                || triggering.metaKey
                || triggering.shiftKey
                || triggering.button === 1;
            if (!modifierClick) {
                return;
            }
            if (evt.detail?.verb !== 'get') {
                return;
            }
            const elt = evt.detail?.elt;
            if (!elt || elt.tagName !== 'A') {
                return;
            }
            if (elt.getAttribute('hx-target') !== 'main' || elt.getAttribute('hx-select') !== 'main') {
                return;
            }
            const url = elt.getAttribute('href') || elt.getAttribute('hx-push-url');
            if (!url || url.startsWith('#')) {
                return;
            }
            evt.preventDefault();
            window.open(url, '_blank');
        });
    }

    setupSeoSync() {
        const fetchSeo = (path) => {
            const seoHead = document.getElementById('seo-head');
            if (!seoHead) {
                return;
            }
            const targetPath = path || window.location.pathname + window.location.search;
            htmx.ajax('GET', '/components/seo?path=' + encodeURIComponent(targetPath), {
                swap: 'none'
            });
        };

        document.body.addEventListener('htmx:afterSettle', (evt) => {
            const target = evt.detail?.target;
            if (!target || target.tagName !== 'MAIN') {
                return;
            }
            // Always refresh: hx-select="main" may skip OOB #seo-head even when present in the response.
            fetchSeo(window.location.pathname + window.location.search);
        });

        document.body.addEventListener('htmx:historyRestore', () => {
            fetchSeo(window.location.pathname + window.location.search);
        });

        document.body.addEventListener('htmx:afterSwap', (evt) => {
            const target = evt.detail?.target;
            if (!target) {
                return;
            }
            if (target.id === 'page-title' || target.id === 'seo-head') {
                const titleEl = document.getElementById('page-title');
                if (titleEl?.textContent) {
                    document.title = titleEl.textContent.trim();
                }
            }
        });
    }

    repairNestedMain() {
        let repaired;
        do {
            repaired = false;
            document.querySelectorAll('main').forEach((outer) => {
                const nested = outer.querySelector(':scope > main');
                if (nested) {
                    outer.replaceWith(nested);
                    repaired = true;
                }
            });
        } while (repaired);
    }

    setupCsrfHeader() {
        document.body.addEventListener('htmx:configRequest', (evt) => {
            const meta = document.querySelector('meta[name="csrf-token"]');
            if (meta && meta.content) {
                evt.detail.headers['X-CSRF-Token'] = meta.content;
            }
        });
    }

    setupErrorHandler() {
        htmx.on('htmx:afterRequest', this.bindErrorMessage);
    }

    bindErrorMessage(evt) {
        const errorElmSelector = evt.target.attributes.getNamedItem('hx-target-error')
        if (errorElmSelector) {
            if (evt.detail.failed) {
                const generic = window.i18n?.t('error.generic.contactAdmin');
                let msg = generic && generic !== 'error.generic.contactAdmin'
                    ? generic
                    : 'Algo deu errado. Entre em contato com o administrador do site';
                if (evt.detail.xhr.responseText) {
                    msg = evt.detail.xhr.responseText;
                }

                const errorElm = document.querySelector(errorElmSelector.value);
                errorElm.textContent = '';
                const parser = new DOMParser();
                const doc = parser.parseFromString(msg, 'text/html');
                const errorMessage = doc.querySelector('.error-message');
                if (errorMessage) {
                    errorElm.appendChild(errorMessage);
                } else {
                    errorElm.textContent = msg;
                }
                const errorMessages = errorElm.querySelectorAll('.error-message');
                errorMessages.forEach(elm => elm.classList.add('visible'));
                window.i18n?.apply(errorElm);
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
        if (evt && evt.detail && evt.detail.target
                && (evt.detail.target.id === 'libraryContent' || evt.detail.target.id === 'savedListContent')) {
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
                    const libraryTab = requestPath.match(/\/writing\/library\/components\/tab\/(drafts|published)(?:\?|$)/);
                    if (libraryTab) {
                        newActiveValue = libraryTab[1];
                    }
                }
                if (!newActiveValue) {
                    const savedTab = requestPath.match(/\/reading\/saved\/components\/tab\/(unread|all)(?:\?|$)/);
                    if (savedTab) {
                        newActiveValue = savedTab[1];
                    }
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
            if (window.codeCopy) {
                window.codeCopy.enhanceAll();
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

    setupSearchModalSubmit() {
        document.body.addEventListener('submit', this.handleSearchModalSubmit);
    }

    handleSearchModalSubmit(event) {
        const form = event.target;
        if (form?.tagName !== 'FORM' || !form.classList.contains('search-form')) {
            return;
        }
        if (!form.closest('#searchModal')) {
            return;
        }
        event.preventDefault();
        const q = (form.querySelector('[name=q]')?.value ?? '').trim();
        const btn = document.getElementById('btnGoToAdvanced');
        if (btn) {
            const path = q ? `/search?q=${encodeURIComponent(q)}` : '/search';
            btn.setAttribute('hx-get', path);
            btn.setAttribute('hx-push-url', path);
            if (typeof htmx !== 'undefined') {
                htmx.process(btn);
            }
        }
        this.closeModal('#btnGoToAdvanced');
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

class HomeGuestMastheadManager {
    static STORAGE_KEY = '__contraponto_home_guest_masthead_dismissed';

    constructor() {
        this.onDismissClick = this.onDismissClick.bind(this);
        this.onAfterSettle = this.onAfterSettle.bind(this);
        document.addEventListener('click', this.onDismissClick);
        document.addEventListener('htmx:afterSettle', this.onAfterSettle);
        this.applyDismissState();
    }

    onAfterSettle(event) {
        const target = event.detail?.target;
        if (!target || (target.id !== 'main-content' && !target.closest?.('main'))) {
            return;
        }
        this.applyDismissState();
    }

    onDismissClick(event) {
        const dismiss = event.target.closest('[data-home-guest-masthead-dismiss]');
        if (!dismiss) {
            return;
        }
        event.preventDefault();
        this.dismiss();
    }

    applyDismissState() {
        const masthead = document.getElementById('home-guest-masthead');
        if (!masthead) {
            return;
        }
        if (this.isDismissed()) {
            masthead.hidden = true;
            masthead.classList.add('u-hidden');
        }
    }

    dismiss() {
        try {
            localStorage.setItem(HomeGuestMastheadManager.STORAGE_KEY, '1');
        } catch (_) {
            /* ignore storage failures */
        }
        const masthead = document.getElementById('home-guest-masthead');
        if (masthead) {
            masthead.hidden = true;
            masthead.classList.add('u-hidden');
        }
    }

    isDismissed() {
        try {
            return localStorage.getItem(HomeGuestMastheadManager.STORAGE_KEY) === '1';
        } catch (_) {
            return false;
        }
    }
}
document.addEventListener('DOMContentLoaded', () => {
    window.main = new MainManager();
    window.imageLightbox = new ImageLightboxManager();
    window.codeCopy = new CodeCopyManager();
    window.codeCopy.enhanceAll();
    window.homeGuestMasthead = new HomeGuestMastheadManager();
});