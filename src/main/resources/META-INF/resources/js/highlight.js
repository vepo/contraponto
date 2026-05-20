/**
 * Post text highlights: selection bar, create/remove, note dialog.
 */
class PostHighlightManager {
    static ARTICLE_SELECTOR = '.article-page__content';
    static ROOT_SELECTOR = '#post-highlights-root';
    static DATA_SCRIPT_ID = 'post-highlights-data';
    static BAR_ID = 'highlights-selection-bar';
    static NOTE_DIALOG_ID = 'highlightNoteDialog';

    constructor() {
        this.root = null;
        this.article = null;
        this.postId = null;
        this.marks = [];
        this.official = [];
        this.selection = null;
        this.selectionRect = null;
        this.barHome = null;
        this.boundSelectionChange = this.onSelectionChange.bind(this);
        this.boundBarClick = this.onBarClick.bind(this);
        this.boundDocumentMouseDown = this.onDocumentMouseDown.bind(this);
        this.boundDocumentKeyDown = this.onDocumentKeyDown.bind(this);
        this.boundHtmxAfterRequest = this.onHtmxAfterRequest.bind(this);
        this.boundHtmxAfterSettle = this.onHtmxAfterSettle.bind(this);
        this.pendingNoteAfterCreate = false;
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
        this.loadData();
        this.applyMarks();
        this.bindSelection();
        this.bindBar();
        this.bindGlobalHandlers();
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

    bindGlobalHandlers() {
        document.removeEventListener('mousedown', this.boundDocumentMouseDown);
        document.addEventListener('mousedown', this.boundDocumentMouseDown);
        document.removeEventListener('keydown', this.boundDocumentKeyDown);
        document.addEventListener('keydown', this.boundDocumentKeyDown);
        document.removeEventListener('htmx:afterRequest', this.boundHtmxAfterRequest);
        document.addEventListener('htmx:afterRequest', this.boundHtmxAfterRequest);
        document.removeEventListener('htmx:afterSettle', this.boundHtmxAfterSettle);
        document.addEventListener('htmx:afterSettle', this.boundHtmxAfterSettle);
    }

    onDocumentMouseDown(evt) {
        const dialog = document.getElementById(PostHighlightManager.NOTE_DIALOG_ID);
        if (dialog && dialog.contains(evt.target)) {
            return;
        }
        if (dialog) {
            this.closeNoteDialog();
        }

        const bar = document.getElementById(PostHighlightManager.BAR_ID);
        if (!bar || bar.hidden) {
            return;
        }
        if (bar.contains(evt.target)) {
            return;
        }
        const article = this.article;
        if (article && article.contains(evt.target)) {
            return;
        }
        this.hideBar();
    }

    onDocumentKeyDown(evt) {
        if (evt.key === 'Escape') {
            if (document.getElementById(PostHighlightManager.NOTE_DIALOG_ID)) {
                this.closeNoteDialog();
                return;
            }
            this.hideBar();
        }
    }

    onSelectionChange() {
        if (document.getElementById(PostHighlightManager.NOTE_DIALOG_ID)) {
            return;
        }
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
        const existing = this.marks.find(m => m.passage === passage || this.normalizePassage(m.passage) === cluster);
        this.selection = {
            passage,
            start,
            end,
            prefix,
            suffix,
            anchorJson: JSON.stringify({ start, end, prefix, suffix }),
            highlightId: existing?.id
        };
        this.showBar(!!existing);
    }

    onBarClick(evt) {
        const action = evt.target.closest('[data-highlight-action]')?.dataset.highlightAction;
        if (!action) {
            return;
        }
        evt.preventDefault();
        evt.stopPropagation();
        if (action === 'sign-in') {
            document.getElementById('loginBtn')?.click();
            return;
        }
        if (action === 'create') {
            this.createHighlight(false);
        } else if (action === 'remove' && this.selection?.highlightId) {
            this.removeHighlight(this.selection.highlightId);
        } else if (action === 'note') {
            this.openNoteFlow();
        }
    }

    openNoteFlow() {
        if (!this.selection) {
            return;
        }
        this.captureSelectionRect();
        const highlightId = this.resolveHighlightId();
        if (highlightId) {
            this.hideBar(false, false);
            this.openNoteDialog(highlightId);
            return;
        }
        this.pendingNoteAfterCreate = true;
        this.createHighlight(true);
    }

    resolveHighlightId() {
        if (this.selection?.highlightId) {
            return this.selection.highlightId;
        }
        if (!this.selection?.passage) {
            return null;
        }
        this.loadData();
        const passage = this.selection.passage;
        const cluster = this.normalizePassage(passage);
        const existing = this.marks.find(m => m.passage === passage || this.normalizePassage(m.passage) === cluster);
        return existing?.id ?? null;
    }

    createHighlight(_silentToast) {
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
        if (!this.pendingNoteAfterCreate) {
            this.hideBar();
            window.getSelection()?.removeAllRanges();
        }
    }

    onHtmxAfterRequest(evt) {
        const xhr = evt.detail?.xhr;
        const path = evt.detail?.pathInfo?.requestPath || '';
        if (!xhr || !path.includes('/highlights')) {
            return;
        }
        const verb = (evt.detail?.requestConfig?.verb || '').toLowerCase();
        if (path.includes('/notes') && evt.detail.successful) {
            this.closeNoteDialog();
            this.hideBar();
            window.getSelection()?.removeAllRanges();
            return;
        }
        if (verb === 'post' && path.includes('/forms/posts/') && path.includes('/highlights') && evt.detail.successful) {
            const highlightId = xhr.getResponseHeader('X-Highlight-Id');
            if (this.pendingNoteAfterCreate && highlightId) {
                this.finishPendingNote(highlightId);
            }
        }
    }

    onHtmxAfterSettle(evt) {
        const highlightsUpdated = evt.detail?.target?.id === 'post-highlights'
            || evt.detail?.target?.querySelector?.(PostHighlightManager.ROOT_SELECTOR);
        if (highlightsUpdated) {
            const pendingPassage = this.pendingNoteAfterCreate ? this.selection?.passage : null;
            this.init();
            if (this.pendingNoteAfterCreate && pendingPassage) {
                this.selection = { passage: pendingPassage };
                this.openPendingNoteFromFragment();
            }
        }
    }

    finishPendingNote(highlightId) {
        this.pendingNoteAfterCreate = false;
        this.openNoteDialog(highlightId);
        this.hideBar(false, false);
        window.getSelection()?.removeAllRanges();
    }

    openPendingNoteFromFragment() {
        if (!this.selection) {
            return;
        }
        const passage = this.selection.passage;
        const cluster = this.normalizePassage(passage);
        const script = document.getElementById(PostHighlightManager.DATA_SCRIPT_ID);
        if (!script) {
            return;
        }
        try {
            const data = JSON.parse(script.textContent);
            const match = (data.marks || []).find(m => m.passage === passage || this.normalizePassage(m.passage) === cluster);
            if (match?.id) {
                this.finishPendingNote(String(match.id));
            }
        } catch (_) {
            /* ignore */
        }
    }

    removeHighlight(id) {
        htmx.ajax('DELETE', '/forms/posts/' + this.postId + '/highlights/' + id, {
            target: '#post-highlights',
            swap: 'innerHTML'
        });
        this.hideBar();
    }

    openNoteDialog(highlightId) {
        this.closeNoteDialog();
        const headers = {};
        const csrf = document.querySelector('meta[name="csrf-token"]')?.content;
        if (csrf) {
            headers['X-CSRF-Token'] = csrf;
        }
        fetch('/forms/highlights/' + highlightId + '/notes/modal', { headers })
            .then(response => response.text())
            .then(html => {
                const wrapper = document.createElement('div');
                wrapper.innerHTML = html.trim();
                const dialog = wrapper.firstElementChild;
                if (!dialog) {
                    return;
                }
                document.body.appendChild(dialog);
                if (typeof htmx !== 'undefined') {
                    htmx.process(dialog);
                }
                window.i18n?.apply(dialog);
                this.positionFloatingElement(dialog, this.getAnchorRect());
                this.bindNoteDialogClose();
                dialog.querySelector('textarea')?.focus();
            });
    }

    closeNoteDialog() {
        document.getElementById(PostHighlightManager.NOTE_DIALOG_ID)?.remove();
    }

    bindNoteDialogClose() {
        const dialog = document.getElementById(PostHighlightManager.NOTE_DIALOG_ID);
        if (!dialog || dialog.dataset.noteDialogBound === 'true') {
            return;
        }
        dialog.dataset.noteDialogBound = 'true';
        dialog.querySelectorAll('[data-highlight-note-close]').forEach(btn => {
            btn.addEventListener('click', () => this.closeNoteDialog());
        });
    }

    captureSelectionRect() {
        const sel = window.getSelection();
        if (sel && sel.rangeCount > 0) {
            this.selectionRect = sel.getRangeAt(0).getBoundingClientRect();
        }
    }

    getAnchorRect() {
        if (this.selectionRect) {
            return this.selectionRect;
        }
        const sel = window.getSelection();
        if (sel && sel.rangeCount > 0) {
            return sel.getRangeAt(0).getBoundingClientRect();
        }
        return {
            top: 80,
            left: 16,
            bottom: 100,
            right: 200,
            width: 184,
            height: 20
        };
    }

    positionFloatingElement(element, anchorRect) {
        const margin = 8;
        let top = anchorRect.bottom + margin;
        let left = anchorRect.left;
        element.style.visibility = 'hidden';
        element.style.display = 'block';
        const elementRect = element.getBoundingClientRect();
        const maxLeft = window.innerWidth - elementRect.width - margin;
        left = Math.max(margin, Math.min(left, maxLeft));
        const maxTop = window.innerHeight - elementRect.height - margin;
        if (top > maxTop) {
            top = Math.max(margin, anchorRect.top - elementRect.height - margin);
        }
        element.style.top = top + 'px';
        element.style.left = left + 'px';
        element.style.visibility = 'visible';
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
        if (!this.barHome) {
            this.barHome = bar.parentElement;
        }
        if (bar.parentElement !== document.body) {
            document.body.appendChild(bar);
        }
        bar.hidden = false;
        bar.classList.remove('u-hidden');
        const createBtn = bar.querySelector('[data-highlight-action="create"]');
        if (createBtn) {
            createBtn.classList.toggle('u-hidden', hasHighlight);
        }
        const removeBtn = bar.querySelector('[data-highlight-action="remove"]');
        if (removeBtn) {
            removeBtn.classList.toggle('u-hidden', !hasHighlight);
        }
        const sel = window.getSelection();
        if (sel && sel.rangeCount > 0) {
            const rect = sel.getRangeAt(0).getBoundingClientRect();
            this.selectionRect = rect;
            this.positionFloatingElement(bar, rect);
            bar.style.display = 'flex';
        }
    }

    hideBar(clearPending = true, clearSelection = true) {
        const bar = document.getElementById(PostHighlightManager.BAR_ID);
        if (bar) {
            bar.hidden = true;
            bar.classList.add('u-hidden');
            bar.style.visibility = '';
            bar.style.display = '';
            if (this.barHome && bar.parentElement === document.body) {
                this.barHome.appendChild(bar);
            }
        }
        if (clearSelection) {
            this.selection = null;
            this.selectionRect = null;
        }
        if (clearPending) {
            this.pendingNoteAfterCreate = false;
        }
    }

    static onAfterSettle(evt) {
        if (evt.detail?.target?.querySelector?.(PostHighlightManager.ROOT_SELECTOR)
            || evt.detail?.target?.id === 'post-highlights'
            || document.querySelector(PostHighlightManager.ROOT_SELECTOR)) {
            PostHighlightManager.instance().init();
        }
    }

    static instance() {
        if (!PostHighlightManager._instance) {
            PostHighlightManager._instance = new PostHighlightManager();
        }
        return PostHighlightManager._instance;
    }
}

PostHighlightManager._instance = null;

document.addEventListener('DOMContentLoaded', () => PostHighlightManager.instance().init());
document.addEventListener('htmx:afterSettle', PostHighlightManager.onAfterSettle);
