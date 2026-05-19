class ReadingTimeTracker {
    constructor() {
        this.intervalId = null;
        this.mainElement = null;
        this.onVisibilityChange = this.onVisibilityChange.bind(this);
        this.onBeforeSwap = this.onBeforeSwap.bind(this);
        this.onAfterSettle = this.onAfterSettle.bind(this);
        this.tick = this.tick.bind(this);

        document.addEventListener('DOMContentLoaded', () => this.onAfterSettle());
        document.addEventListener('htmx:afterSettle', this.onAfterSettle);
        document.addEventListener('htmx:beforeSwap', this.onBeforeSwap);
    }

    onAfterSettle() {
        this.stop();
        const main = document.querySelector('main.article-page[data-reading-time-post-id]');
        if (!main) {
            return;
        }
        const url = main.getAttribute('data-reading-time-url');
        if (!url) {
            return;
        }
        this.mainElement = main;
        this.readingTimeUrl = url;
        document.addEventListener('visibilitychange', this.onVisibilityChange);
        this.intervalId = window.setInterval(this.tick, 5000);
    }

    onBeforeSwap(event) {
        const target = event.detail && event.detail.target;
        if (!target) {
            return;
        }
        if (target.id === 'main-content' || target.tagName === 'MAIN' || target.querySelector?.('main.article-page')) {
            this.stop();
        }
    }

    onVisibilityChange() {
        if (document.visibilityState === 'visible') {
            if (this.mainElement && !this.intervalId) {
                this.intervalId = window.setInterval(this.tick, 5000);
            }
        } else if (this.intervalId) {
            window.clearInterval(this.intervalId);
            this.intervalId = null;
        }
    }

    tick() {
        if (document.visibilityState !== 'visible' || !this.readingTimeUrl) {
            return;
        }
        const meta = document.querySelector('meta[name="csrf-token"]');
        const headers = { 'X-CSRF-Token': meta && meta.content ? meta.content : '' };
        fetch(this.readingTimeUrl, { method: 'POST', headers, credentials: 'same-origin' })
            .catch(() => {});
    }

    stop() {
        if (this.intervalId) {
            window.clearInterval(this.intervalId);
            this.intervalId = null;
        }
        document.removeEventListener('visibilitychange', this.onVisibilityChange);
        this.mainElement = null;
        this.readingTimeUrl = null;
    }
}

new ReadingTimeTracker();
