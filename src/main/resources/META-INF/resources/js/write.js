// src/main/resources/META-INF/resources/js/write.js

class WriteEditor {
    constructor() {
        // this.init();
        this.setupRichText = this.setupRichText.bind(this);
        this.setupRichTextButton = this.setupRichTextButton.bind(this);
        this.commandButtonCallback = this.commandButtonCallback.bind(this);
        document.body.addEventListener('htmx:afterSettle', this.setupRichText);
        this.setupRichText();
    }

    setupRichText() {
        const editorToolbar = document.getElementById('editorToolbar');
        if (editorToolbar) {
            editorToolbar.querySelectorAll('button').forEach(this.setupRichTextButton);
        }
    }

    setupRichTextButton(commandBtn) {
        commandBtn.addEventListener('click', this.commandButtonCallback);
    }

    commandButtonCallback(evt) {
        const button = evt.target;
        const command = button.getAttribute('data-command');
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
                replacement = `<u>${selectedText || 'underlined text'}</u>`;
                break;
            case 'h2':
                replacement = `\n## ${selectedText || 'Heading 2'}\n`;
                break;
            case 'h3':
                replacement = `\n### ${selectedText || 'Heading 3'}\n`;
                break;
            case 'ul':
                replacement = selectedText.split('\n').map(line => `- ${line}`).join('\n');
                break;
            case 'ol':
                replacement = selectedText.split('\n').map((line, i) => `${i + 1}. ${line}`).join('\n');
                break;
            case 'blockquote':
                replacement = `> ${selectedText || 'quote text'}`;
                break;
            case 'code':
                replacement = `\`${selectedText || 'code'}\``;
                break;
            case 'link':
                const url = prompt('Enter URL:', 'https://');
                if (url) {
                    replacement = `[${selectedText || 'link text'}](${url})`;
                }
                break;
            default:
                return;
        }
        
        editor.value = beforeText + replacement + afterText;
        editor.focus();
        
        // Set cursor position
        const newCursorPos = start + replacement.length;
        editor.setSelectionRange(newCursorPos, newCursorPos);
        
        // Trigger input event
        editor.dispatchEvent(new Event('input'));
    }
}

// Initialize editor when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new WriteEditor();
});