/**
 * Post text highlights: selection bar, create/remove, inline marks.
 */
class PostHighlightManager {
    static ARTICLE_SELECTOR = '.article-page__content';
    static ROOT_SELECTOR = '#post-highlights-root';
    static DATA_SCRIPT_ID = 'post-highlights-data';
    static BAR_ID = 'highlights-selection-bar';

    constructor() {
        this.root = null;
        this.article = null;
        this.postId = null;
        this.highlightsUrl = null;
        this.marks = [];
        this.official = [];
        this.selection = null;
        this.boundSelectionChange = this.onSelectionChange.bind(this);
        this.boundBarClick = this.onBarClick.bind(this);
    }

    init() {
        this.root = document.querySelector(PostHighlightManager.ROOT_SELECTOR);
        if (!this.root) {
            return;
        }
        this.article = document.querySelector(PostHighlightManager.ARTICLE_SELECTOR);
        if (!this.article) {
            return;
        }
        this.postId = this.root.dataset.postId;
        this.highlightsUrl = this.root.dataset.highlightsUrl;
        this.loadData();
        this.applyMarks();
        this.bindSelection();
        this.bindBar();
    }

    loadData() {
        const script = document.getElementById(PostHighlightManager.DATA_SCRIPT_ID);
        if (!script) {
            return;
        }
        try {
            const data = JSON.parse(script.textContent);
            this.marks = data.marks || [];
            this.official = data.official || [];
        } catch (e) {
            console.warn('Could not parse highlights data', e);
        }
    }

    bindSelection() {
        document.removeEventListener('mouseup', this.boundSelectionChange);
        document.addEventListener('mouseup', this.boundSelectionChange);
    }

    bindBar() {
        const bar = document.getElementById(PostHighlightManager.BAR_ID);
        if (!bar || bar.dataset.highlightBarBound === 'true') {
            return;
        }
        bar.dataset.highlightBarBound = 'true';
        bar.addEventListener('click', this.boundBarClick);
    }

    onSelectionChange() {
        const sel = window.getSelection();
        if (!sel || sel.isCollapsed || !this.article) {
            this.hideBar();
            return;
        }
        const range = sel.getRangeAt(0);
        if (!this.article.contains(range.commonAncestorContainer)) {
            this.hideBar();
            return;
        }
        const passage = sel.toString().trim();
        if (!passage) {
            this.hideBar();
            return;
        }
        const start = this.offsetInArticle(range.startContainer, range.startOffset);
        const end = this.offsetInArticle(range.endContainer, range.endOffset);
        if (start < 0 || end <= start) {
            this.hideBar();
            return;
        }
        const prefix = this.article.textContent.substring(Math.max(0, start - 32), start);
        const suffix = this.article.textContent.substring(end, Math.min(this.article.textContent.length, end + 32));
        const cluster = this.normalizePassage(passage);
        const existing = this.marks.find(m => m.cluster === cluster || m.passage === passage);
        this.selection = { passage, start, end, prefix, suffix, anchorJson: JSON.stringify({ start, end, prefix, suffix }), highlightId: existing?.id };
        this.showBar(!!existing);
    }

    onBarClick(evt) {
        const action = evt.target.closest('[data-highlight-action]')?.dataset.highlightAction;
        if (!action) {
            return;
        }
        evt.preventDefault();
        if (action === 'sign-in') {
            document.getElementById('loginBtn')?.click();
            return;
        }
        if (action === 'create') {
            this.createHighlight();
        } else if (action === 'remove' && this.selection?.highlightId) {
            this.removeHighlight(this.selection.highlightId);
        } else if (action === 'note' && this.selection?.highlightId) {
            const body = window.prompt(window.i18n?.t('highlight.note.prompt') || 'Nota (opcional):');
            if (body === null) {
                return;
            }
            const makePublic = window.confirm(window.i18n?.t('highlight.note.makePublic') || 'Tornar nota pública?');
            this.saveNote(this.selection.highlightId, body, makePublic);
        }
    }

    createHighlight() {
        if (!this.selection) {
            return;
        }
        const form = new URLSearchParams();
        form.set('passage', this.selection.passage);
        form.set('anchorJson', this.selection.anchorJson);
        htmx.ajax('POST', '/forms/posts/' + this.postId + '/highlights', {
            target: '#post-highlights',
            swap: 'innerHTML',
            values: Object.fromEntries(form)
        });
        this.hideBar();
        window.getSelection()?.removeAllRanges();
    }

    removeHighlight(id) {
        htmx.ajax('DELETE', '/forms/posts/' + this.postId + '/highlights/' + id, {
            target: '#post-highlights',
            swap: 'innerHTML'
        });
        this.hideBar();
    }

    saveNote(highlightId, body, makePublic) {
        const form = new URLSearchParams();
        form.set('body', body);
        form.set('makePublic', makePublic ? 'true' : 'false');
        htmx.ajax('POST', '/forms/highlights/' + highlightId + '/notes', {
            swap: 'none',
            values: Object.fromEntries(form)
        });
    }

    applyMarks() {
        this.clearMarks();
        const all = [];
        for (const o of this.official) {
            all.push({ ...o, official: true });
        }
        for (const m of this.marks.filter(m => !m.official)) {
            all.push({ ...m, official: false });
        }
        for (const item of all) {
            try {
                const anchor = typeof item.anchorJson === 'string' ? JSON.parse(item.anchorJson) : item.anchorJson;
                this.wrapRange(anchor.start, anchor.end, item.official);
            } catch (_) {
                /* anchor may be stale */
            }
        }
    }

    wrapRange(start, end, official) {
        const text = this.article.textContent;
        if (start < 0 || end > text.length || start >= end) {
            return;
        }
        const walker = document.createTreeWalker(this.article, NodeFilter.SHOW_TEXT);
        let pos = 0;
        let startNode = null;
        let startOff = 0;
        let endNode = null;
        let endOff = 0;
        while (walker.nextNode()) {
            const node = walker.currentNode;
            const len = node.textContent.length;
            if (!startNode && pos + len > start) {
                startNode = node;
                startOff = start - pos;
            }
            if (!endNode && pos + len >= end) {
                endNode = node;
                endOff = end - pos;
                break;
            }
            pos += len;
        }
        if (!startNode || !endNode) {
            return;
        }
        const range = document.createRange();
        range.setStart(startNode, startOff);
        range.setEnd(endNode, endOff);
        const mark = document.createElement('mark');
        mark.className = official ? 'post-highlight post-highlight--official' : 'post-highlight post-highlight--personal';
        if (official) {
            const badge = document.createElement('span');
            badge.className = 'post-highlight__badge';
            badge.textContent = window.i18n?.t('highlight.official.badge') || 'Destaque do autor';
            mark.appendChild(badge);
        }
        try {
            range.surroundContents(mark);
        } catch (_) {
            /* overlapping marks */
        }
    }

    clearMarks() {
        this.article.querySelectorAll('mark.post-highlight').forEach(el => {
            const parent = el.parentNode;
            while (el.firstChild) {
                parent.insertBefore(el.firstChild, el);
            }
            parent.removeChild(el);
        });
    }

    offsetInArticle(node, offset) {
        const range = document.createRange();
        range.selectNodeContents(this.article);
        range.setEnd(node, offset);
        return range.toString().length;
    }

    normalizePassage(text) {
        return text.trim().toLowerCase().replace(/\s+/g, ' ');
    }

    showBar(hasHighlight) {
        const bar = document.getElementById(PostHighlightManager.BAR_ID);
        if (!bar) {
            return;
        }
        bar.hidden = false;
        bar.classList.remove('u-hidden');
        const removeBtn = bar.querySelector('[data-highlight-action="remove"]');
        if (removeBtn) {
            removeBtn.classList.toggle('u-hidden', !hasHighlight);
        }
        const sel = window.getSelection();
        if (sel && sel.rangeCount > 0) {
            const rect = sel.getRangeAt(0).getBoundingClientRect();
            bar.style.top = (window.scrollY + rect.bottom + 8) + 'px';
            bar.style.left = (window.scrollX + rect.left) + 'px';
        }
    }

    hideBar() {
        const bar = document.getElementById(PostHighlightManager.BAR_ID);
        if (!bar) {
            return;
        }
        bar.hidden = true;
        bar.classList.add('u-hidden');
        this.selection = null;
    }

    static onAfterSettle(evt) {
        if (evt.detail?.target?.querySelector?.(PostHighlightManager.ROOT_SELECTOR)
            || evt.detail?.target?.id === 'post-highlights'
            || document.querySelector(PostHighlightManager.ROOT_SELECTOR)) {
            new PostHighlightManager().init();
        }
    }
}

document.addEventListener('DOMContentLoaded', () => new PostHighlightManager().init());
document.body.addEventListener('htmx:afterSettle', PostHighlightManager.onAfterSettle);
