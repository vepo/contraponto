class WriteEditor {
    constructor() {
        this.isPreviewMode = false;
        this.currentMode = 'markdown';   // 'markdown' or 'asciidoc'
        this.setupRichText = this.setupRichText.bind(this);
        this.commandButtonCallback = this.commandButtonCallback.bind(this);
        this.togglePreview = this.togglePreview.bind(this);
        this.renderPreview = this.renderPreview.bind(this);
        this.changeMode = this.changeMode.bind(this);
        document.body.addEventListener('htmx:afterSettle', this.setupRichText);
        this.setupRichText();
    }

    setupRichText() {
        const editorToolbar = document.getElementById('editorToolbar');
        if (editorToolbar) {
            editorToolbar.addEventListener('click', (e) => {
                const button = e.target.closest('[data-command]');
                if (button) this.commandButtonCallback({ target: button });
            });
        }

        // Custom mode dropdown
        const modeButton = document.getElementById('editorModeButton');
        const modeDropdown = document.getElementById('editorModeDropdown');
        const modeOptions = document.querySelectorAll('.editor-mode-option');

        if (modeButton) {
            modeButton.addEventListener('click', (e) => {
                e.stopPropagation();
                const wrapper = modeButton.closest('.editor-mode-wrapper');
                wrapper.classList.toggle('open');
            });
        }

        modeOptions.forEach(option => {
            option.addEventListener('click', (e) => {
                const mode = option.getAttribute('data-mode');
                this.changeMode(mode);
                // Update button label and icon
                const icon = option.querySelector('.editor-mode-option-icon').innerHTML;
                const label = option.querySelector('.editor-mode-option-label').innerHTML;
                document.querySelector('.editor-mode-icon').innerHTML = icon;
                document.getElementById('currentModeLabel').innerText = label;
                // Close dropdown
                document.querySelector('.editor-mode-wrapper').classList.remove('open');
            });
        });

        // Close dropdown if clicking outside
        document.addEventListener('click', (e) => {
            const wrapper = document.querySelector('.editor-mode-wrapper');
            if (wrapper && !wrapper.contains(e.target)) {
                wrapper.classList.remove('open');
            }
        });
        // Set initial hint
        this.updateHint();
    }

    changeMode(mode) {
        this.currentMode = mode;
        document.getElementById('format').value = mode;
        this.updateHint();
        if (this.isPreviewMode) this.renderPreview();
    }

    updateHint() {
        const hint = document.getElementById('editorHint');
        if (hint) {
            if (this.currentMode === 'markdown') {
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

        // Helper to get line boundaries
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

        if (this.currentMode === 'markdown') {
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
        } else { // AsciiDoc mode
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

    async insertImageMarkdown(editor) {
        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.accept = 'image/jpeg,image/png,image/gif,image/webp';
        fileInput.onchange = async (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const formData = new FormData();
            formData.append('file', file);
            try {
                const res = await fetch('/api/images', {
                    method: 'POST',
                    headers: { 'X-CSRF-Token': document.querySelector('meta[name="csrf-token"]').content },
                    body: formData
                });
                const img = await res.json();
                const markdownImage = `![${file.name}](${img.url})`;
                const start = editor.selectionStart;
                const end = editor.selectionEnd;
                editor.value = editor.value.substring(0, start) + markdownImage + editor.value.substring(end);
                editor.setSelectionRange(start + markdownImage.length, start + markdownImage.length);
                editor.dispatchEvent(new Event('input'));
            } catch (err) {
                alert('Image upload failed');
            }
        };
        fileInput.click();
    }

    async insertImageAsciiDoc(editor) {
        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.accept = 'image/jpeg,image/png,image/gif,image/webp';
        fileInput.onchange = async (e) => {
            const file = e.target.files[0];
            if (!file) return;
            const formData = new FormData();
            formData.append('file', file);
            try {
                const res = await fetch('/api/images', {
                    method: 'POST',
                    headers: { 'X-CSRF-Token': document.querySelector('meta[name="csrf-token"]').content },
                    body: formData
                });
                const img = await res.json();
                const asciidocImage = `image::${img.url}[${file.name}]`;
                const start = editor.selectionStart;
                const end = editor.selectionEnd;
                editor.value = editor.value.substring(0, start) + asciidocImage + editor.value.substring(end);
                editor.setSelectionRange(start + asciidocImage.length, start + asciidocImage.length);
                editor.dispatchEvent(new Event('input'));
            } catch (err) {
                alert('Image upload failed');
            }
        };
        fileInput.click();
    }

    togglePreview() {
        this.isPreviewMode = !this.isPreviewMode;
        const editor = document.getElementById('content');
        const previewDiv = document.getElementById('previewContainer');
        const toolbar = document.getElementById('editorToolbar');
        const toggleBtn = document.getElementById('previewToggleBtn');

        if (this.isPreviewMode) {
            this.renderPreview();
            editor.style.display = 'none';
            previewDiv.style.display = 'block';
            toolbar.classList.add('preview-mode');
            toggleBtn.textContent = '✏️ Edit';
        } else {
            editor.style.display = 'block';
            previewDiv.style.display = 'none';
            toolbar.classList.remove('preview-mode');
            toggleBtn.textContent = '👁️ Preview';
        }
    }

    renderPreview() {
        const editor = document.getElementById('content');
        const previewDiv = document.getElementById('previewContainer');
        const rawText = editor.value;

        if (this.currentMode === 'markdown') {
            if (typeof marked !== 'undefined') {
                marked.setOptions({ breaks: true, gfm: true, headerIds: false, mangle: false });
                previewDiv.innerHTML = marked.parse(rawText);
                if (typeof hljs !== 'undefined') {
                    hljs.highlightAll();
                }
            } else {
                previewDiv.innerHTML = '<p>Markdown preview not available.</p>';
            }
        }
        else { // AsciiDoc
            if (typeof Asciidoctor !== 'undefined') {
                const asciidoctor = Asciidoctor();
                const html = asciidoctor.convert(rawText, { safe: 'safe', attributes: { showtitle: true } });
                previewDiv.innerHTML = html;
                if (typeof hljs !== 'undefined') {
                    hljs.highlightAll();
                }
            } else {
                previewDiv.innerHTML = '<p>AsciiDoc preview not available. Please refresh the page.</p>';
            }
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new WriteEditor();
});