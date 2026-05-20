/**
 * Post text highlights: selection bar, create/remove, note dialog, action bar.
 */
class PostHighlightManager {
    static ARTICLE_SELECTOR = '.article-page__content';
    static ROOT_SELECTOR = '#post-highlights-root';
    static DATA_SCRIPT_ID = 'post-highlights-data';
    static BAR_ID = 'highlights-selection-bar';
    static ACTION_BAR_ID = 'highlights-action-bar';
    static NOTE_DIALOG_ID = 'highlightNoteDialog';
    static NOTE_TOOLTIP_ID = 'post-highlight-note-tooltip';

    constructor() {
        this.root = null;
        this.article = null;
        this.postId = null;
        this.marks = [];
        this.official = [];
        this.selection = null;
        this.selectionRect = null;
        this.actionContext = null;
        this.selectionBar = null;
        this.actionBar = null;
        this.barHome = null;
        this.actionBarHome = null;
        this.boundSelectionChange = this.onSelectionChange.bind(this);
        this.boundBarClick = this.onBarClick.bind(this);
        this.boundArticleClick = this.onArticleClick.bind(this);
        this.boundRootClick = this.onRootClick.bind(this);
        this.boundDocumentMouseDown = this.onDocumentMouseDown.bind(this);
        this.boundDocumentKeyDown = this.onDocumentKeyDown.bind(this);
        this.boundHtmxAfterRequest = this.onHtmxAfterRequest.bind(this);
        this.boundHtmxAfterSettle = this.onHtmxAfterSettle.bind(this);
        this.boundHtmxBeforeSwap = this.onHtmxBeforeSwap.bind(this);
        this.boundScroll = this.onScroll.bind(this);
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
        this.resetFloatingBars();
        this.refreshBarRefs();
        this.loadData();
        this.applyMarks();
        this.bindSelection();
        this.bindBars();
        this.bindSignInActions();
        this.bindInteractive();
        this.bindGlobalHandlers();
    }

    resetFloatingBars() {
        for (const barId of [PostHighlightManager.BAR_ID, PostHighlightManager.ACTION_BAR_ID]) {
            document.querySelectorAll('#' + barId).forEach(el => {
                if (el.parentElement === document.body) {
                    el.remove();
                }
            });
        }
        this.hideNoteTooltip();
        this.barHome = null;
        this.actionBarHome = null;
        this.selectionBar = null;
        this.actionBar = null;
    }

    refreshBarRefs() {
        if (!this.root) {
            return;
        }
        this.selectionBar = this.root.querySelector('#' + PostHighlightManager.BAR_ID);
        this.actionBar = this.root.querySelector('#' + PostHighlightManager.ACTION_BAR_ID);
    }

    resolveBar(barId) {
        const bars = [...document.querySelectorAll('#' + barId)];
        if (bars.length === 0) {
            return null;
        }
        if (bars.length === 1) {
            return bars[0];
        }
        const rootBar = this.root?.querySelector('#' + barId);
        const active = bars.find(bar => !bar.hidden) ?? rootBar ?? bars[bars.length - 1];
        for (const bar of bars) {
            if (bar !== active && bar.parentElement === document.body) {
                bar.remove();
            }
        }
        if (barId === PostHighlightManager.BAR_ID) {
            this.selectionBar = active;
        } else if (barId === PostHighlightManager.ACTION_BAR_ID) {
            this.actionBar = active;
        }
        return active;
    }

    selectionBarElement() {
        if (this.selectionBar?.isConnected) {
            return this.selectionBar;
        }
        this.selectionBar = this.resolveBar(PostHighlightManager.BAR_ID);
        return this.selectionBar;
    }

    actionBarElement() {
        if (this.actionBar?.isConnected) {
            return this.actionBar;
        }
        this.actionBar = this.resolveBar(PostHighlightManager.ACTION_BAR_ID);
        return this.actionBar;
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

    bindBars() {
        for (const bar of [this.selectionBarElement(), this.actionBarElement()]) {
            if (!bar || bar.dataset.highlightBarBound === 'true') {
                continue;
            }
            bar.dataset.highlightBarBound = 'true';
            bar.addEventListener('click', this.boundBarClick);
        }
    }

    bindSignInActions() {
        if (!this.root || this.root.dataset.highlightSignInBound === 'true') {
            return;
        }
        this.root.dataset.highlightSignInBound = 'true';
        this.root.addEventListener('click', evt => {
            const btn = evt.target.closest('[data-highlight-action="sign-in"]');
            if (!btn || !this.root.contains(btn)) {
                return;
            }
            evt.preventDefault();
            evt.stopPropagation();
            this.openSignInModal();
        });
    }

    bindInteractive() {
        if (this.article && this.article.dataset.highlightArticleBound !== 'true') {
            this.article.dataset.highlightArticleBound = 'true';
            this.article.addEventListener('click', this.boundArticleClick);
        }
        if (this.root && this.root.dataset.highlightRootBound !== 'true') {
            this.root.dataset.highlightRootBound = 'true';
            this.root.addEventListener('click', this.boundRootClick);
        }
        this.root?.querySelectorAll('.highlight-note-card--interactive').forEach(card => {
            if (card.dataset.highlightNoteBound === 'true') {
                return;
            }
            card.dataset.highlightNoteBound = 'true';
            card.addEventListener('click', evt => this.onNoteCardClick(evt, card));
        });
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
        document.removeEventListener('htmx:beforeSwap', this.boundHtmxBeforeSwap);
        document.addEventListener('htmx:beforeSwap', this.boundHtmxBeforeSwap);
        window.removeEventListener('scroll', this.boundScroll, true);
        window.addEventListener('scroll', this.boundScroll, true);
    }

    onHtmxBeforeSwap(evt) {
        const target = evt.detail?.target;
        if (!target) {
            return;
        }
        if (target.id === 'post-highlights' || target.tagName === 'MAIN') {
            this.hideBar(false, false);
            this.hideActionBar();
            this.hideNoteTooltip();
            this.closeNoteDialog();
            this.resetFloatingBars();
        }
    }

    onScroll() {
        const bar = this.selectionBarElement();
        if (bar && !bar.hidden) {
            this.hideBar(false, false);
        }
    }

    openSignInModal() {
        this.hideBar(false, false);
        const headerLogin = document.querySelector('button.btn--auth-login');
        if (headerLogin) {
            headerLogin.click();
            return;
        }
        if (typeof htmx !== 'undefined') {
            htmx.ajax('GET', '/auth/modal?mode=login', {
                target: '#modal-container',
                swap: 'innerHTML'
            });
        }
    }

    onDocumentMouseDown(evt) {
        const dialog = document.getElementById(PostHighlightManager.NOTE_DIALOG_ID);
        if (dialog && dialog.contains(evt.target)) {
            return;
        }
        if (dialog) {
            this.closeNoteDialog();
        }

        const actionBar = this.actionBarElement();
        if (actionBar && !actionBar.hidden) {
            if (actionBar.contains(evt.target)) {
                return;
            }
            if (evt.target.closest?.('mark.post-highlight--interactive, .highlight-note-card--interactive')) {
                return;
            }
            this.hideActionBar();
        }

        const bar = this.selectionBarElement();
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
        if (evt.key !== 'Escape') {
            return;
        }
        if (document.getElementById(PostHighlightManager.NOTE_DIALOG_ID)) {
            this.closeNoteDialog();
            return;
        }
        if (!this.actionBarElement()?.hidden) {
            this.hideActionBar();
            return;
        }
        this.hideBar();
    }

    onMarkClick(evt, mark) {
        evt.preventDefault();
        evt.stopPropagation();
        this.hideNoteTooltip();
        window.getSelection()?.removeAllRanges();
        this.hideBar(false);
        this.closeNoteDialog();
        this.showActionBar('mark', mark.getBoundingClientRect(), { highlightId: mark.dataset.highlightId });
    }

    onArticleClick(evt) {
        const mark = evt.target.closest('mark.post-highlight--interactive[data-highlight-id]');
        if (!mark) {
            return;
        }
        this.onMarkClick(evt, mark);
    }

    onNoteCardClick(evt, card) {
        evt.preventDefault();
        evt.stopPropagation();
        window.getSelection()?.removeAllRanges();
        this.hideBar(false);
        this.closeNoteDialog();
        this.showActionBar('note', card.getBoundingClientRect(), {
            highlightId: card.dataset.highlightId,
            noteId: card.dataset.highlightNoteId
        });
    }

    onRootClick(evt) {
        const card = evt.target.closest('.highlight-note-card--interactive');
        if (!card) {
            return;
        }
        this.onNoteCardClick(evt, card);
    }

    onSelectionChange(evt) {
        if (document.getElementById(PostHighlightManager.NOTE_DIALOG_ID)) {
            return;
        }
        if (evt?.target?.closest?.('mark.post-highlight--interactive, .highlight-note-card--interactive')) {
            return;
        }
        this.hideActionBar();
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
        if (action === 'remove-mark' || action === 'remove-note') {
            return;
        }
        evt.preventDefault();
        evt.stopPropagation();
        if (action === 'sign-in') {
            this.openSignInModal();
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
            this.hideActionBar();
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
            values: Object.fromEntries(form),
            headers: this.csrfHeaders()
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
        if (!evt.detail.successful) {
            return;
        }
        if (verb === 'delete') {
            this.closeNoteDialog();
            this.hideBar();
            this.hideActionBar();
            window.getSelection()?.removeAllRanges();
            return;
        }
        if (path.includes('/notes')) {
            this.closeNoteDialog();
            this.hideBar();
            this.hideActionBar();
            window.getSelection()?.removeAllRanges();
        }
        if (verb === 'post' && path.includes('/forms/posts/') && path.includes('/highlights')) {
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
            swap: 'innerHTML',
            headers: this.csrfHeaders()
        });
    }

    removeNote(highlightId, noteId) {
        htmx.ajax('DELETE', '/forms/highlights/' + highlightId + '/notes/' + noteId, {
            target: '#post-highlights',
            swap: 'innerHTML',
            headers: this.csrfHeaders()
        });
    }

    csrfHeaders() {
        const csrf = document.querySelector('meta[name="csrf-token"]')?.content;
        return csrf ? { 'X-CSRF-Token': csrf } : {};
    }

    openNoteDialog(highlightId) {
        this.closeNoteDialog();
        this.hideActionBar();
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
        const display = element.classList.contains('highlights-selection-bar') ? 'flex' : 'block';
        element.style.display = display;
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

    showActionBar(type, anchorRect, context) {
        const bar = this.actionBarElement();
        if (!bar) {
            return;
        }
        this.actionContext = context;
        if (!this.actionBarHome) {
            this.actionBarHome = bar.parentElement;
        }
        if (bar.parentElement !== document.body) {
            document.body.appendChild(bar);
        }
        bar.dataset.removeHighlightId = context.highlightId || '';
        bar.dataset.removeNoteId = context.noteId || '';
        bar.dataset.removeNoteHighlightId = context.highlightId || '';
        bar.hidden = false;
        bar.classList.remove('u-hidden');
        const markBtn = bar.querySelector('[data-highlight-action="remove-mark"]');
        const noteBtn = bar.querySelector('[data-highlight-action="remove-note"]');
        if (markBtn) {
            markBtn.classList.toggle('u-hidden', type !== 'mark');
            markBtn.onclick = type === 'mark'
                ? evt => {
                    evt.preventDefault();
                    evt.stopPropagation();
                    this.removeHighlight(context.highlightId);
                    this.hideActionBar();
                }
                : null;
        }
        if (noteBtn) {
            noteBtn.classList.toggle('u-hidden', type !== 'note');
            noteBtn.onclick = type === 'note'
                ? evt => {
                    evt.preventDefault();
                    evt.stopPropagation();
                    this.removeNote(context.highlightId, context.noteId);
                    this.hideActionBar();
                }
                : null;
        }
        this.positionFloatingElement(bar, anchorRect);
    }

    hideActionBar() {
        const bar = this.actionBarElement();
        if (bar) {
            bar.hidden = true;
            bar.classList.add('u-hidden');
            bar.style.visibility = '';
            bar.style.display = '';
            if (this.actionBarHome && bar.parentElement === document.body) {
                this.actionBarHome.appendChild(bar);
            }
        }
        this.actionContext = null;
    }

    applyMarks() {
        this.clearMarks();
        const all = [];
        for (const o of this.official) {
            all.push({ ...o, official: true, interactive: false });
        }
        for (const m of this.marks) {
            if (m.official || !m.ownHighlight) {
                continue;
            }
            all.push({ ...m, official: false, interactive: true });
        }
        for (const item of all) {
            try {
                const anchor = typeof item.anchorJson === 'string' ? JSON.parse(item.anchorJson) : item.anchorJson;
                this.wrapRange(anchor.start, anchor.end, item.official, item.interactive ? item.id : null, item.notePreview);
            } catch (_) {
                /* anchor may be stale */
            }
        }
    }

    wrapRange(start, end, official, highlightId, notePreview) {
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
        const hasNote = notePreview && String(notePreview).trim();
        if (official) {
            mark.className = 'post-highlight post-highlight--official';
        } else if (hasNote) {
            mark.className = 'post-highlight post-highlight--noted';
        } else {
            mark.className = 'post-highlight post-highlight--personal';
        }
        if (official) {
            const badge = document.createElement('span');
            badge.className = 'post-highlight__badge';
            badge.textContent = window.i18n?.t('highlight.official.badge') || 'Destaque do autor';
            mark.appendChild(badge);
        }
        if (highlightId) {
            mark.dataset.highlightId = String(highlightId);
            mark.classList.add('post-highlight--interactive');
            mark.tabIndex = 0;
            mark.setAttribute('role', 'button');
            mark.addEventListener('click', evt => this.onMarkClick(evt, mark));
            if (hasNote) {
                mark.addEventListener('mouseenter', () => this.showNoteTooltip(mark, notePreview));
                mark.addEventListener('mouseleave', () => this.hideNoteTooltip());
                mark.addEventListener('focus', () => this.showNoteTooltip(mark, notePreview));
                mark.addEventListener('blur', () => this.hideNoteTooltip());
            }
        }
        try {
            range.surroundContents(mark);
            this.tagDropCapIfNeeded(mark);
        } catch (_) {
            /* overlapping marks */
        }
    }

    tagDropCapIfNeeded(mark) {
        if (this.markAffectsDropCap(mark)) {
            mark.classList.add('post-highlight--affects-drop-cap');
        }
    }

    markAffectsDropCap(mark) {
        const paragraph = mark.closest('p');
        if (!paragraph || !this.article?.contains(paragraph)) {
            return false;
        }
        const dropCapParagraph = this.article.querySelector(':scope > p:first-of-type')
            ?? this.article.querySelector(':scope > .paragraph:first-of-type > p:first-of-type');
        if (paragraph !== dropCapParagraph) {
            return false;
        }
        const range = document.createRange();
        range.setStart(paragraph, 0);
        range.setEnd(mark, 0);
        return range.toString().trim().length === 0;
    }

    showNoteTooltip(mark, notePreview) {
        this.hideNoteTooltip();
        const text = String(notePreview || '').trim();
        if (!text) {
            return;
        }
        const tooltip = document.createElement('div');
        tooltip.id = PostHighlightManager.NOTE_TOOLTIP_ID;
        tooltip.className = 'post-highlight-note-tooltip';
        tooltip.setAttribute('role', 'tooltip');
        tooltip.textContent = text;
        document.body.appendChild(tooltip);
        this.positionFloatingElement(tooltip, mark.getBoundingClientRect());
    }

    hideNoteTooltip() {
        document.getElementById(PostHighlightManager.NOTE_TOOLTIP_ID)?.remove();
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
        this.hideActionBar();
        const bar = this.selectionBarElement();
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
        }
    }

    hideBar(clearPending = true, clearSelection = true) {
        const bar = this.selectionBarElement();
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
