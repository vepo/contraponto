class WriteEditor {
    constructor() {
        this.isPreviewMode = false;
        this.setupRichText = this.setupRichText.bind(this);
        this.commandButtonCallback = this.commandButtonCallback.bind(this);
        this.togglePreview = this.togglePreview.bind(this);
        this.renderPreview = this.renderPreview.bind(this);
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
    }

    commandButtonCallback(evt) {
        const button = evt.target.closest('[data-command]');
        if (!button) return;

        const command = button.getAttribute('data-command');

        // Handle preview toggle specially
        if (command === 'togglePreview') {
            this.togglePreview();
            return;
        }

        // Ignore formatting commands when in preview mode
        if (this.isPreviewMode) return;

        const editor = document.getElementById('content');
        const start = editor.selectionStart;
        const end = editor.selectionEnd;
        const selectedText = editor.value.substring(start, end);
        const beforeText = editor.value.substring(0, start);
        const afterText = editor.value.substring(end);

        let replacement = '';

        switch (command) {
            case 'bold':
                replacement = `**${selectedText || 'bold text'}**`;
                break;
            case 'italic':
                replacement = `*${selectedText || 'italic text'}*`;
                break;
            case 'underline':
                replacement = `++${selectedText || 'underlined text'}++`;
                break;
            case 'h2':
                replacement = `\n## ${selectedText || 'Heading 2'}\n`;
                break;
            case 'h3':
                replacement = `\n### ${selectedText || 'Heading 3'}\n`;
                break;
            case 'ul':
                if (!selectedText) {
                    replacement = '- list item\n';
                } else {
                    replacement = selectedText.split('\n').map(line => `- ${line}`).join('\n');
                }
                break;
            case 'ol':
                if (!selectedText) {
                    replacement = '1. list item\n';
                } else {
                    replacement = selectedText.split('\n').map((line, i) => `${i + 1}. ${line}`).join('\n');
                }
                break;
            case 'blockquote':
                replacement = `> ${selectedText || 'quote text'}`;
                break;
            case 'code':
                replacement = `\`${selectedText || 'code'}\``;
                break;
            case 'link':
                const url = prompt('Enter URL:', 'https://');
                if (!url) return;
                replacement = `[${selectedText || 'link text'}](${url})`;
                break;
            case 'image':
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
                return;
            default:
                return;
        }

        editor.value = beforeText + replacement + afterText;
        editor.focus();
        const newCursorPos = start + replacement.length;
        editor.setSelectionRange(newCursorPos, newCursorPos);
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
        const markdownText = editor.value;

        // Configure marked for safe HTML (optional)
        if (typeof marked !== 'undefined') {
            marked.setOptions({
                breaks: true,
                gfm: true,
                headerIds: false,
                mangle: false
            });
            previewDiv.innerHTML = marked.parse(markdownText);
            hljs.highlightAll();
        } else {
            previewDiv.innerHTML = '<p>Markdown preview not available. Please refresh the page.</p>';
        }
    }
}

// Initialize editor when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new WriteEditor();
});