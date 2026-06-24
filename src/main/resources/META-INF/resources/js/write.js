class WriteEditor {
    static WRITE_PATH = /^\/write(\/draft\/\d+)?(\?.*)?$/;

    constructor() {
        this.isPreviewMode = false;
        this.currentMode = 'MARKDOWN';
        this.mountedToolbar = null;
        this.mountedModeButton = null;
        this.mountedModeDropdown = null;
        this.mountedForm = null;
        this.baseline = null;
        this.pendingNavigation = null;
        this.onBeforeSwap = this.onBeforeSwap.bind(this);
        this.onAfterSettle = this.onAfterSettle.bind(this);
        this.onDocumentClick = this.onDocumentClick.bind(this);
        this.onToolbarClick = this.onToolbarClick.bind(this);
        this.onModeButtonClick = this.onModeButtonClick.bind(this);
        this.onModeDropdownClick = this.onModeDropdownClick.bind(this);
        this.onFormInput = this.onFormInput.bind(this);
        this.onHtmxConfirm = this.onHtmxConfirm.bind(this);
        this.onBeforeUnload = this.onBeforeUnload.bind(this);
        this.onSaveDraftAfterRequest = this.onSaveDraftAfterRequest.bind(this);
        this.commandButtonCallback = this.commandButtonCallback.bind(this);
        this.togglePreview = this.togglePreview.bind(this);
        this.renderPreview = this.renderPreview.bind(this);
        this.changeMode = this.changeMode.bind(this);
        this.onLeaveSave = this.onLeaveSave.bind(this);
        this.onLeaveDiscard = this.onLeaveDiscard.bind(this);
        this.onLeaveCancel = this.onLeaveCancel.bind(this);

        document.addEventListener('htmx:beforeSwap', this.onBeforeSwap);
        document.body.addEventListener('htmx:afterSettle', this.onAfterSettle);
        document.body.addEventListener('htmx:confirm', this.onHtmxConfirm);
        document.body.addEventListener('htmx:afterRequest', this.onSaveDraftAfterRequest);
        document.addEventListener('click', this.onDocumentClick);
        window.addEventListener('beforeunload', this.onBeforeUnload);
        this.tryMount();
    }

    isMounted() {
        return this.mountedToolbar != null && document.body.contains(this.mountedToolbar);
    }

    isOnWritePage() {
        return WriteEditor.WRITE_PATH.test(window.location.pathname);
    }

    isLeavingWriteRequest(detail) {
        const path = (detail?.path || '').split('?')[0];
        if (!path) {
            return false;
        }
        const verb = (detail.verb || 'get').toLowerCase();
        if (verb !== 'get') {
            return false;
        }
        if (path.startsWith('/forms/') || path.startsWith('/api/')) {
            return false;
        }
        return !WriteEditor.WRITE_PATH.test(path);
    }

    isDirty() {
        if (!this.baseline) {
            return false;
        }
        const current = this.captureFormSnapshot();
        return JSON.stringify(current) !== JSON.stringify(this.baseline);
    }

    captureFormSnapshot() {
        return {
            title: document.getElementById('title')?.value ?? '',
            content: document.getElementById('content')?.value ?? '',
            slug: document.getElementById('slug')?.value ?? '',
            description: document.getElementById('description')?.value ?? '',
            format: document.getElementById('format')?.value ?? '',
            tagsJson: document.getElementById('tagsJson')?.value ?? '[]',
            coverId: document.getElementById('coverId')?.value ?? '',
            serieTitle: document.getElementById('serieTitle')?.value ?? '',
            blogId: this.readBlogId()
        };
    }

    readBlogId() {
        const select = document.getElementById('blogSelect');
        if (select && !select.disabled) {
            return select.value ?? '';
        }
        const hidden = document.querySelector('#postForm input[name="blogId"]');
        return hidden?.value ?? '';
    }

    captureBaseline() {
        this.baseline = this.captureFormSnapshot();
    }

    onBeforeSwap(evt) {
        const target = evt.detail?.target;
        if (target?.tagName === 'MAIN') {
            this.unmount();
        }
    }

    onAfterSettle(evt) {
        const target = evt.detail?.target;
        if (target && target.tagName !== 'MAIN' && !target.closest?.('main')) {
            return;
        }
        this.tryMount();
    }

    tryMount() {
        const toolbar = document.getElementById('editorToolbar');
        if (!toolbar) {
            this.unmount();
            return;
        }
        if (toolbar === this.mountedToolbar) {
            return;
        }
        this.unmount();
        this.mountedToolbar = toolbar;
        this.isPreviewMode = false;

        toolbar.addEventListener('click', this.onToolbarClick);

        const modeButton = document.getElementById('editorModeButton');
        const modeDropdown = document.getElementById('editorModeDropdown');
        if (modeButton) {
            this.mountedModeButton = modeButton;
            modeButton.addEventListener('click', this.onModeButtonClick);
        }
        if (modeDropdown) {
            this.mountedModeDropdown = modeDropdown;
            modeDropdown.addEventListener('click', this.onModeDropdownClick);
        }

        const form = document.getElementById('postForm');
        if (form) {
            this.mountedForm = form;
            form.addEventListener('input', this.onFormInput);
            form.addEventListener('change', this.onFormInput);
        }

        this.bindLeaveModal();
        this.syncModeFromForm();
        this.updateHint();
        this.captureBaseline();
    }

    unmount() {
        if (this.mountedToolbar) {
            this.mountedToolbar.removeEventListener('click', this.onToolbarClick);
            delete this.mountedToolbar.dataset.writeEditorBound;
            this.mountedToolbar = null;
        }
        if (this.mountedModeButton) {
            this.mountedModeButton.removeEventListener('click', this.onModeButtonClick);
            this.mountedModeButton = null;
        }
        if (this.mountedModeDropdown) {
            this.mountedModeDropdown.removeEventListener('click', this.onModeDropdownClick);
            this.mountedModeDropdown = null;
        }
        if (this.mountedForm) {
            this.mountedForm.removeEventListener('input', this.onFormInput);
            this.mountedForm.removeEventListener('change', this.onFormInput);
            this.mountedForm = null;
        }
        this.isPreviewMode = false;
        this.baseline = null;
        this.hideLeaveModal();
        this.pendingNavigation = null;
    }

    onFormInput() {
        /* dirty state derived from baseline comparison */
    }

    onHtmxConfirm(evt) {
        if (!this.isOnWritePage() || !this.isDirty()) {
            return;
        }
        const detail = evt.detail;
        if (!detail || !this.isLeavingWriteRequest(detail)) {
            return;
        }
        evt.preventDefault();
        this.pendingNavigation = detail;
        this.showLeaveModal();
    }

    onBeforeUnload(evt) {
        if (this.isOnWritePage() && this.isDirty()) {
            evt.preventDefault();
            evt.returnValue = '';
        }
    }

    onSaveDraftAfterRequest(evt) {
        const elt = evt.detail?.requestConfig?.elt;
        if (!elt || elt.id !== 'saveDraft') {
            return;
        }
        if (evt.detail.failed) {
            return;
        }
        this.captureBaseline();
        if (this.pendingNavigation?.issueRequest) {
            const pending = this.pendingNavigation;
            this.pendingNavigation = null;
            this.hideLeaveModal();
            pending.issueRequest(true);
        }
    }

    bindLeaveModal() {
        document.getElementById('writeLeaveSaveBtn')?.addEventListener('click', this.onLeaveSave);
        document.getElementById('writeLeaveDiscardBtn')?.addEventListener('click', this.onLeaveDiscard);
        document.getElementById('writeLeaveCancelBtn')?.addEventListener('click', this.onLeaveCancel);
    }

    showLeaveModal() {
        const modal = document.getElementById('writeLeaveModal');
        if (!modal) {
            return;
        }
        modal.classList.add('modal--open');
        window.i18n?.apply(modal);
    }

    hideLeaveModal() {
        const modal = document.getElementById('writeLeaveModal');
        if (modal) {
            modal.classList.remove('modal--open');
        }
    }

    onLeaveSave() {
        const saveBtn = document.getElementById('saveDraft');
        if (!saveBtn) {
            return;
        }
        if (typeof htmx !== 'undefined') {
            htmx.trigger(saveBtn, 'click');
        } else {
            saveBtn.click();
        }
    }

    onLeaveDiscard() {
        const pending = this.pendingNavigation;
        this.pendingNavigation = null;
        this.baseline = this.captureFormSnapshot();
        this.hideLeaveModal();
        if (pending?.issueRequest) {
            pending.issueRequest(true);
        }
    }

    onLeaveCancel() {
        this.pendingNavigation = null;
        this.hideLeaveModal();
    }

    syncModeFromForm() {
        const formatInput = document.getElementById('format');
        const mode = formatInput?.value?.trim()?.toUpperCase();
        if (mode === 'MARKDOWN' || mode === 'ASCIIDOC') {
            this.currentMode = mode;
        }
        this.updateModeButtonFromCurrentMode();
    }

    updateModeButtonFromCurrentMode() {
        const option = document.querySelector(`.editor-mode-option[data-mode="${this.currentMode}"]`);
        if (option) {
            this.updateModeButtonLabel(option);
            return;
        }
        const label = document.getElementById('currentModeLabel');
        if (label) {
            label.innerText = this.currentMode === 'ASCIIDOC' ? 'AsciiDoc' : 'Markdown';
        }
    }

    updateModeButtonLabel(option) {
        const icon = option.querySelector('.editor-mode-option-icon')?.innerHTML;
        const label = option.querySelector('.editor-mode-option-label')?.innerHTML;
        const iconEl = document.querySelector('#editorModeButton .editor-mode-icon');
        const labelEl = document.getElementById('currentModeLabel');
        if (icon && iconEl) {
            iconEl.innerHTML = icon;
        }
        if (label && labelEl) {
            labelEl.innerText = label;
        }
    }

    onToolbarClick(e) {
        const button = e.target.closest('[data-command]');
        if (button) {
            this.commandButtonCallback({ target: button });
        }
    }

    onModeButtonClick(e) {
        e.stopPropagation();
        e.currentTarget.closest('.editor-mode-wrapper')?.classList.toggle('open');
    }

    onModeDropdownClick(e) {
        const option = e.target.closest('.editor-mode-option');
        if (!option) {
            return;
        }
        const mode = option.getAttribute('data-mode');
        this.changeMode(mode);
        this.updateModeButtonLabel(option);
        document.querySelector('.editor-mode-wrapper')?.classList.remove('open');
    }

    onDocumentClick(e) {
        const wrapper = document.querySelector('.editor-mode-wrapper');
        if (wrapper && !wrapper.contains(e.target)) {
            wrapper.classList.remove('open');
        }
    }

    changeMode(mode) {
        this.currentMode = mode;
        document.getElementById('format').value = mode;
        this.updateHint();
        if (this.isPreviewMode) {
            this.renderPreview();
        }
    }

    updateHint() {
        const hint = document.getElementById('editorHint');
        if (hint) {
            if (this.currentMode === 'MARKDOWN') {
                hint.innerHTML = 'Markdown formatting supported • Use **bold**, *italic*, [links](url), etc.';
            } else {
                hint.innerHTML = 'AsciiDoc formatting supported • Use *bold*, _italic_, link:url[text], etc.';
            }
        }
    }

    commandButtonCallback(evt) {
        const button = evt.target.closest('[data-command]');
        if (!button) return;
        const command = button.getAttribute('data-command');
        if (command === 'togglePreview') {
            this.togglePreview();
            return;
        }
        if (this.isPreviewMode) return;

        const editor = document.getElementById('content');
        const start = editor.selectionStart;
        const end = editor.selectionEnd;
        const selectedText = editor.value.substring(start, end);
        const beforeText = editor.value.substring(0, start);
        const afterText = editor.value.substring(end);

        const getLineBounds = (text, pos) => {
            const lineStart = text.lastIndexOf('\n', pos - 1) + 1;
            const lineEnd = text.indexOf('\n', pos);
            return { lineStart, lineEnd: lineEnd === -1 ? text.length : lineEnd };
        };
        const lineBounds = getLineBounds(editor.value, start);
        const isAtLineStart = start === lineBounds.lineStart;
        const isAtLineEnd = (end === lineBounds.lineEnd) || (end === editor.value.length);
        const isBlockCommand = ['h2', 'h3', 'blockquote', 'ul', 'ol'].includes(command);

        let leading = '';
        let trailing = '';
        if (isBlockCommand && !isAtLineStart) leading = '\n';
        if (isBlockCommand && !isAtLineEnd) trailing = '\n';

        let replacement = '';

        if (this.currentMode === 'MARKDOWN') {
            switch (command) {
                case 'bold': replacement = `**${selectedText || 'bold text'}**`; break;
                case 'italic': replacement = `*${selectedText || 'italic text'}*`; break;
                case 'underline': replacement = `++${selectedText || 'underlined text'}++`; break;
                case 'h2': replacement = `${leading}## ${selectedText || 'Heading 2'}${trailing}`; break;
                case 'h3': replacement = `${leading}### ${selectedText || 'Heading 3'}${trailing}`; break;
                case 'ul':
                    if (!selectedText) {
                        replacement = `${leading}- list item${trailing}`;
                    } else {
                        replacement = selectedText.split('\n').map(l => `- ${l}`).join('\n');
                        if (!isAtLineStart) replacement = `\n${replacement}`;
                        if (!isAtLineEnd && !replacement.endsWith('\n')) replacement = `${replacement}\n`;
                    }
                    break;
                case 'ol':
                    if (!selectedText) {
                        replacement = `${leading}1. list item${trailing}`;
                    } else {
                        replacement = selectedText.split('\n').map((l, i) => `${i + 1}. ${l}`).join('\n');
                        if (!isAtLineStart) replacement = `\n${replacement}`;
                        if (!isAtLineEnd && !replacement.endsWith('\n')) replacement = `${replacement}\n`;
                    }
                    break;
                case 'blockquote': replacement = `${leading}> ${selectedText || 'quote text'}${trailing}`; break;
                case 'code': replacement = `\`${selectedText || 'code'}\``; break;
                case 'link':
                    const url = prompt('Enter URL:', 'https://');
                    if (!url) return;
                    replacement = `[${selectedText || 'link text'}](${url})`;
                    break;
                case 'image':
                    this.insertImageMarkdown(editor);
                    return;
                default: return;
            }
        } else {
            switch (command) {
                case 'bold': replacement = `*${selectedText || 'bold text'}*`; break;
                case 'italic': replacement = `_${selectedText || 'italic text'}_`; break;
                case 'underline': replacement = `[.underline]#${selectedText || 'underlined text'}#`; break;
                case 'h2': replacement = `${leading}== ${selectedText || 'Heading 2'}${trailing}`; break;
                case 'h3': replacement = `${leading}=== ${selectedText || 'Heading 3'}${trailing}`; break;
                case 'ul':
                    if (!selectedText) {
                        replacement = `${leading}* list item${trailing}`;
                    } else {
                        replacement = selectedText.split('\n').map(l => `* ${l}`).join('\n');
                        if (!isAtLineStart) replacement = `\n${replacement}`;
                        if (!isAtLineEnd && !replacement.endsWith('\n')) replacement = `${replacement}\n`;
                    }
                    break;
                case 'ol':
                    if (!selectedText) {
                        replacement = `${leading}. list item${trailing}`;
                    } else {
                        replacement = selectedText.split('\n').map((l, i) => `. ${l}`).join('\n');
                        if (!isAtLineStart) replacement = `\n${replacement}`;
                        if (!isAtLineEnd && !replacement.endsWith('\n')) replacement = `${replacement}\n`;
                    }
                    break;
                case 'blockquote': replacement = `${leading}____\n${selectedText || 'quote text'}\n____${trailing}`; break;
                case 'code': replacement = `\`${selectedText || 'code'}\``; break;
                case 'link':
                    const linkUrl = prompt('Enter URL:', 'https://');
                    if (!linkUrl) return;
                    replacement = `link:${linkUrl}[${selectedText || 'link text'}]`;
                    break;
                case 'image':
                    this.insertImageAsciiDoc(editor);
                    return;
                default: return;
            }
        }

        editor.value = beforeText + replacement + afterText;
        editor.focus();
        const newCursorPos = start + replacement.length;
        editor.setSelectionRange(newCursorPos, newCursorPos);
        editor.dispatchEvent(new Event('input'));
    }

    insertImageMarkdown(editor) {
        if (!window.imagePicker) {
            console.warn('Image picker is not available');
            return;
        }
        window.imagePicker.open({
            onSelect: (image) => this.insertImageAtCaret(editor, `![](${image.url})`)
        });
    }

    insertImageAsciiDoc(editor) {
        if (!window.imagePicker) {
            console.warn('Image picker is not available');
            return;
        }
        window.imagePicker.open({
            onSelect: (image) => this.insertImageAtCaret(editor, `image::${image.url}[]`)
        });
    }

    insertImageAtCaret(editor, snippet) {
        const start = editor.selectionStart;
        const end = editor.selectionEnd;
        editor.value = editor.value.substring(0, start) + snippet + editor.value.substring(end);
        editor.setSelectionRange(start + snippet.length, start + snippet.length);
        editor.dispatchEvent(new Event('input'));
    }

    togglePreview() {
        this.isPreviewMode = !this.isPreviewMode;
        const editor = document.getElementById('content');
        const previewDiv = document.getElementById('previewContainer');
        const toolbar = document.getElementById('editorToolbar');
        const toggleBtn = document.getElementById('previewToggleBtn');

        if (this.isPreviewMode) {
            this.renderPreview();
            editor.classList.add('u-hidden');
            previewDiv.classList.remove('u-hidden');
            toolbar.classList.add('preview-mode');
            toggleBtn.textContent = '✏️ Edit';
        } else {
            editor.classList.remove('u-hidden');
            previewDiv.classList.add('u-hidden');
            toolbar.classList.remove('preview-mode');
            toggleBtn.textContent = '👁️ Preview';
        }
    }

    renderPreview() {
        const editor = document.getElementById('content');
        const previewDiv = document.getElementById('previewContainer');
        const rawText = editor.value;

        if (this.currentMode === 'MARKDOWN') {
            if (typeof marked !== 'undefined') {
                marked.setOptions({ breaks: true, gfm: true, headerIds: false, mangle: false });
                previewDiv.innerHTML = marked.parse(rawText);
                this.highlightPreview(previewDiv);
            } else {
                previewDiv.innerHTML = '<p>Markdown preview not available.</p>';
            }
        } else {
            if (typeof Asciidoctor !== 'undefined') {
                const asciidoctor = Asciidoctor();
                const html = asciidoctor.convert(rawText, {
                    safe: 'safe',
                    attributes: { showtitle: true, 'figure-caption!': '' }
                });
                previewDiv.innerHTML = html;
                this.highlightPreview(previewDiv);
            } else {
                previewDiv.innerHTML = '<p>AsciiDoc preview not available. Please refresh the page.</p>';
            }
        }
    }

    highlightPreview(previewDiv) {
        if (typeof hljs !== 'undefined') {
            previewDiv.querySelectorAll('pre code').forEach(block => hljs.highlightElement(block));
        }
        if (window.codeCopy) {
            window.codeCopy.enhanceAll();
        }
    }
}

class WriteTagsPicker {
    constructor() {
        this.mountedRoot = null;
        this.onBeforeSwap = this.onBeforeSwap.bind(this);
        this.onAfterSettle = this.onAfterSettle.bind(this);
        document.addEventListener('htmx:beforeSwap', this.onBeforeSwap);
        document.body.addEventListener('htmx:afterSettle', this.onAfterSettle);
        this.tryMount();
    }

    onBeforeSwap(evt) {
        const target = evt.detail?.target;
        if (target?.tagName === 'MAIN') {
            this.unmount();
        }
    }

    onAfterSettle(evt) {
        const target = evt.detail?.target;
        if (target && target.tagName !== 'MAIN' && !target.closest?.('main')) {
            return;
        }
        this.tryMount();
    }

    tryMount() {
        const root = document.getElementById('tagsPicker');
        if (!root) {
            this.unmount();
            return;
        }
        if (root === this.mountedRoot) {
            return;
        }
        this.unmount();
        this.mountedRoot = root;
        this.chipsEl = document.getElementById('tagsChips');
        this.input = document.getElementById('tagInput');
        this.hidden = document.getElementById('tagsJson');
        this.datalist = document.getElementById('tagSuggestionsList');
        if (!this.chipsEl || !this.input || !this.hidden || !this.datalist) {
            this.unmount();
            return;
        }
        this.labels = this.parseInitial(this.hidden.value);
        this.renderChips();
        this.onKeydown = (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                this.commitCurrent();
            }
        };
        this.onInput = () => {
            clearTimeout(this.suggestTimer);
            this.suggestTimer = setTimeout(() => this.refreshSuggestions(), 180);
        };
        this.input.addEventListener('keydown', this.onKeydown);
        this.input.addEventListener('input', this.onInput);
    }

    unmount() {
        if (this.input && this.onKeydown) {
            this.input.removeEventListener('keydown', this.onKeydown);
            this.input.removeEventListener('input', this.onInput);
        }
        if (this.mountedRoot) {
            delete this.mountedRoot.dataset.tagsPickerInit;
            this.mountedRoot = null;
        }
        clearTimeout(this.suggestTimer);
        this.suggestTimer = null;
        this.onKeydown = null;
        this.onInput = null;
    }

    parseInitial(json) {
        try {
            const arr = JSON.parse(json || '[]');
            if (!Array.isArray(arr)) return [];
            return arr.map((s) => String(s).trim()).filter(Boolean);
        } catch {
            return [];
        }
    }

    serialize() {
        this.hidden.value = JSON.stringify(this.labels);
    }

    renderChips() {
        this.chipsEl.innerHTML = '';
        this.labels.forEach((label, idx) => {
            const chip = document.createElement('span');
            chip.className = 'write-tags__chip';
            const labelSpan = document.createElement('span');
            labelSpan.textContent = label;
            const rm = document.createElement('button');
            rm.type = 'button';
            rm.className = 'write-tags__chip-remove';
            rm.setAttribute('aria-label', 'Remove tag');
            rm.textContent = '×';
            rm.addEventListener('click', () => {
                this.labels.splice(idx, 1);
                this.renderChips();
                this.serialize();
            });
            chip.appendChild(labelSpan);
            chip.appendChild(rm);
            this.chipsEl.appendChild(chip);
        });
        this.serialize();
    }

    commitCurrent() {
        const v = (this.input.value || '').trim();
        if (!v) return;
        const lower = v.toLowerCase();
        if (!this.labels.some((t) => t.toLowerCase() === lower)) {
            this.labels.push(v);
        }
        this.input.value = '';
        this.renderChips();
        this.refreshSuggestions();
    }

    async refreshSuggestions() {
        if (!this.datalist) return;
        this.datalist.innerHTML = '';
        const q = (this.input.value || '').trim();
        try {
            const res = await fetch('/forms/write/tag-suggestions?q=' + encodeURIComponent(q), {
                headers: { Accept: 'application/json' },
                credentials: 'same-origin'
            });
            if (!res.ok) return;
            const names = await res.json();
            if (!Array.isArray(names)) return;
            names.forEach((n) => {
                const opt = document.createElement('option');
                opt.value = n;
                this.datalist.appendChild(opt);
            });
        } catch {
            /* ignore */
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (document.querySelector('script[src*="asciidoctor.min.js"]')) {
        initWriteModules();
    }
});

document.addEventListener('contraponto:assets-ready', (evt) => {
    if (evt.detail?.profile === 'write') {
        initWriteModules();
    }
});

function initWriteModules() {
    if (!window.writeEditor) {
        window.writeEditor = new WriteEditor();
    }
    if (!window.writeTagsPicker) {
        window.writeTagsPicker = new WriteTagsPicker();
    }
    window.writeEditor.tryMount();
}
