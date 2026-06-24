/**
 * Loads optional page asset bundles after HTMX navigation (head is not reloaded).
 */
class AssetLoader {
    static WRITE_PATH = /^\/write(\/draft\/\d+)?(\?.*)?$/;

    static MANAGE_PREFIXES = [
        '/manage',
        '/writing',
        '/library',
        '/administration',
        '/review',
        '/editor',
        '/blogs/',
        '/users'
    ];

    static POST_SCRIPTS = [
        '/js/reading-time.js',
        '/js/highlight.js',
        '/js/third-party/highlight.min.js'
    ];

    static WRITE_SCRIPTS = [
        '/js/reading-time.js',
        '/js/highlight.js',
        '/js/image-picker.js',
        '/js/write.js',
        '/js/image-upload.js',
        '/js/third-party/highlight.min.js',
        '/js/third-party/languages/java.min.js',
        '/js/third-party/languages/yaml.min.js',
        '/js/third-party/languages/json.min.js',
        '/js/third-party/languages/bash.min.js',
        '/js/third-party/languages/graphql.min.js',
        '/js/third-party/languages/xml.min.js',
        '/js/third-party/languages/protobuf.min.js',
        '/js/third-party/languages/dockerfile.min.js',
        '/js/third-party/marked.min.js',
        '/js/third-party/asciidoctor.min.js'
    ];

    constructor() {
        this.writeLoadPromise = null;
        this.postLoadPromise = null;
        this.manageLoadPromise = null;
        this.onBeforeRequest = this.onBeforeRequest.bind(this);
        this.onAfterSettle = this.onAfterSettle.bind(this);
        this.onHoverWrite = this.onHoverWrite.bind(this);
        document.body.addEventListener('htmx:beforeRequest', this.onBeforeRequest);
        document.body.addEventListener('htmx:afterSettle', this.onAfterSettle);
        document.addEventListener('mouseover', this.onHoverWrite, true);
        document.addEventListener('DOMContentLoaded', () => {
            void this.ensureForPath(window.location.pathname);
        });
    }

    hasStylesheet(href) {
        return document.querySelector(`link[rel="stylesheet"][href="${href}"]`) != null;
    }

    hasScript(src) {
        return document.querySelector(`script[src="${src}"]`) != null;
    }

    loadStylesheet(href) {
        if (this.hasStylesheet(href)) {
            return Promise.resolve();
        }
        return new Promise((resolve, reject) => {
            const link = document.createElement('link');
            link.rel = 'stylesheet';
            link.href = href;
            link.onload = () => resolve();
            link.onerror = () => reject(new Error(`Failed to load stylesheet ${href}`));
            document.head.appendChild(link);
        });
    }

    loadScript(src) {
        if (this.hasScript(src)) {
            return Promise.resolve();
        }
        return new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.src = src;
            script.defer = true;
            script.onload = () => resolve();
            script.onerror = () => reject(new Error(`Failed to load script ${src}`));
            document.head.appendChild(script);
        });
    }

    async loadScriptsSequential(sources) {
        for (const src of sources) {
            if (!this.hasScript(src)) {
                await this.loadScript(src);
            }
        }
    }

    writeAssetsPresent() {
        return this.hasScript('/js/third-party/asciidoctor.min.js');
    }

    postAssetsPresent() {
        return this.hasScript('/js/third-party/highlight.min.js')
            && this.hasScript('/js/highlight.js');
    }

    manageAssetsPresent() {
        return this.hasStylesheet('/style/manage.css');
    }

    ensureManageAssets() {
        if (this.manageAssetsPresent()) {
            return Promise.resolve();
        }
        if (!this.manageLoadPromise) {
            this.manageLoadPromise = this.loadStylesheet('/style/manage.css')
                .then(() => this.dispatchReady('manage'))
                .catch((error) => {
                    this.manageLoadPromise = null;
                    console.warn(error);
                });
        }
        return this.manageLoadPromise;
    }

    ensurePostAssets() {
        if (this.postAssetsPresent()) {
            return Promise.resolve();
        }
        if (!this.postLoadPromise) {
            this.postLoadPromise = Promise.all([
                this.loadStylesheet('/style/third-party/default.min.css'),
                this.loadScriptsSequential(AssetLoader.POST_SCRIPTS)
            ]).then(() => this.dispatchReady('post'))
              .catch((error) => {
                  this.postLoadPromise = null;
                  console.warn(error);
              });
        }
        return this.postLoadPromise;
    }

    ensureWriteAssets() {
        if (this.writeAssetsPresent()) {
            return Promise.resolve();
        }
        if (!this.writeLoadPromise) {
            this.writeLoadPromise = Promise.all([
                this.loadStylesheet('/style/write.css'),
                this.loadStylesheet('/style/third-party/default.min.css'),
                this.loadScriptsSequential(AssetLoader.WRITE_SCRIPTS)
            ]).then(() => this.dispatchReady('write'))
              .catch((error) => {
                  this.writeLoadPromise = null;
                  console.warn(error);
              });
        }
        return this.writeLoadPromise;
    }

    dispatchReady(profile) {
        document.dispatchEvent(new CustomEvent('contraponto:assets-ready', { detail: { profile } }));
    }

    isManagePath(path) {
        return AssetLoader.MANAGE_PREFIXES.some((prefix) => path.startsWith(prefix));
    }

    requestPath(detail) {
        return (detail?.path || '').split('?')[0];
    }

    onBeforeRequest(evt) {
        const path = this.requestPath(evt.detail);
        const verb = (evt.detail?.verb || 'get').toLowerCase();
        if (verb !== 'get' || !path) {
            return;
        }
        if (AssetLoader.WRITE_PATH.test(path)) {
            void this.ensureWriteAssets();
            return;
        }
        if (path.includes('/post/')) {
            void this.ensurePostAssets();
            return;
        }
        if (this.isManagePath(path)) {
            void this.ensureManageAssets();
        }
    }

    onAfterSettle(evt) {
        void this.ensureForPath(window.location.pathname);
    }

    onHoverWrite(evt) {
        const trigger = evt.target.closest?.('[data-hx-get="/write"], [data-hx-get^="/write/"]');
        if (trigger) {
            void this.ensureWriteAssets();
        }
    }

    ensureForPath(path) {
        if (AssetLoader.WRITE_PATH.test(path)) {
            return this.ensureWriteAssets();
        }
        if (path.includes('/post/')) {
            return this.ensurePostAssets();
        }
        if (this.isManagePath(path)) {
            return this.ensureManageAssets();
        }
        return Promise.resolve();
    }
}

window.assetLoader = new AssetLoader();
