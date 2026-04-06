// src/main/resources/META-INF/resources/js/write.js

class WriteEditor {
    constructor() {
        this.isAuthenticated = false;
        this.currentUser = null;
        this.postId = null;
        this.isPublished = false;
        this.init();
    }
    
    async init() {
        // Wait for auth to be ready
        if (window.authManager) {
            this.isAuthenticated = window.authManager.isAuthenticated;
            this.currentUser = window.authManager.currentUser;
            
            if (!this.isAuthenticated) {
                // Redirect to home if not authenticated
                window.location.href = '/';
                return;
            }
            
            this.setupEventListeners();
            this.setupToolbar();
            this.setupCoverUpload();
            this.setupSlugGeneration();
            this.loadExistingPost();
            this.updateUserMenu();
        } else {
            // Wait for authManager to load
            setTimeout(() => this.init(), 100);
        }
    }
    
    setupEventListeners() {
        this.saveDraftBtn = document.getElementById('saveDraftBtn');
        this.publishBtn = document.getElementById('publishBtn');
        this.postForm = document.getElementById('postForm');
        this.titleInput = document.getElementById('title');
        this.slugInput = document.getElementById('slug');
        
        if (this.saveDraftBtn) {
            this.saveDraftBtn.addEventListener('click', () => this.savePost(false));
        }
        
        if (this.publishBtn) {
            this.publishBtn.addEventListener('click', () => this.savePost(true));
        }
    }
    
    setupToolbar() {
        const toolbar = document.getElementById('editorToolbar');
        const editor = document.getElementById('content');
        
        if (!toolbar || !editor) return;
        
        toolbar.addEventListener('click', (e) => {
            const button = e.target.closest('[data-command]');
            if (!button) return;
            
            const command = button.getAttribute('data-command');
            this.executeEditorCommand(command, editor);
        });
    }
    
    executeEditorCommand(command, editor) {
        const textarea = editor;
        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const selectedText = textarea.value.substring(start, end);
        const beforeText = textarea.value.substring(0, start);
        const afterText = textarea.value.substring(end);
        
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
        
        textarea.value = beforeText + replacement + afterText;
        textarea.focus();
        
        // Set cursor position
        const newCursorPos = start + replacement.length;
        textarea.setSelectionRange(newCursorPos, newCursorPos);
        
        // Trigger input event
        textarea.dispatchEvent(new Event('input'));
    }
    
    setupCoverUpload() {
        const coverArea = document.getElementById('coverArea');
        const coverInput = document.getElementById('coverInput');
        const coverPlaceholder = document.getElementById('coverPlaceholder');
        const coverPreview = document.getElementById('coverPreview');
        const coverImage = document.getElementById('coverImage');
        const removeCoverBtn = document.getElementById('removeCoverBtn');
        
        if (coverArea) {
            coverArea.addEventListener('click', () => {
                coverInput.click();
            });
        }
        
        if (coverInput) {
            coverInput.addEventListener('change', async (e) => {
                const file = e.target.files[0];
                if (file) {
                    await this.uploadCover(file);
                }
            });
        }
        
        if (removeCoverBtn) {
            removeCoverBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.removeCover();
            });
        }
    }
    
    async uploadCover(file) {
        const formData = new FormData();
        formData.append('file', file);
        
        this.showToast('Uploading cover image...');
        
        try {
            const response = await window.authManager.authenticatedFetch('/api/images', {
                method: 'POST',
                body: formData
            });
            
            if (response.ok) {
                const image = await response.json();
                document.getElementById('coverId').value = image.id;
                this.updateCoverPreview(image.url);
                this.showToast('Cover image uploaded!', 'success');
            } else {
                const error = await response.json();
                this.showToast(error.error || 'Failed to upload image', 'error');
            }
        } catch (error) {
            console.error('Upload error:', error);
            this.showToast('Failed to upload image', 'error');
        }
    }
    
    updateCoverPreview(url) {
        const coverPlaceholder = document.getElementById('coverPlaceholder');
        const coverPreview = document.getElementById('coverPreview');
        const coverImage = document.getElementById('coverImage');
        
        if (coverImage) {
            coverImage.src = url;
        }
        
        if (coverPlaceholder) coverPlaceholder.style.display = 'none';
        if (coverPreview) coverPreview.style.display = 'block';
    }
    
    removeCover() {
        document.getElementById('coverId').value = '';
        
        const coverPlaceholder = document.getElementById('coverPlaceholder');
        const coverPreview = document.getElementById('coverPreview');
        
        if (coverPlaceholder) coverPlaceholder.style.display = 'flex';
        if (coverPreview) coverPreview.style.display = 'none';
    }
    
    setupSlugGeneration() {
        const titleInput = document.getElementById('title');
        const slugInput = document.getElementById('slug');
        const slugPreview = document.getElementById('slugPreview');
        
        if (titleInput && slugInput) {
            titleInput.addEventListener('input', () => {
                if (!slugInput.value || slugInput.value === this.generateSlug(titleInput.value)) {
                    const slug = this.generateSlug(titleInput.value);
                    slugInput.value = slug;
                    if (slugPreview) {
                        slugPreview.textContent = `/post/${slug}`;
                    }
                }
            });
        }
        
        if (slugInput && slugPreview) {
            slugInput.addEventListener('input', () => {
                if (slugPreview) {
                    slugPreview.textContent = `/post/${slugInput.value}`;
                }
            });
        }
    }
    
    generateSlug(text) {
        return text
            .toLowerCase()
            .trim()
            .replace(/[^\w\s-]/g, '')
            .replace(/[\s_-]+/g, '-')
            .replace(/^-+|-+$/g, '');
    }
    
    async loadExistingPost() {
        const urlParams = new URLSearchParams(window.location.search);
        const editId = urlParams.get('edit');
        
        if (editId) {
            this.postId = parseInt(editId);
            await this.fetchPost(this.postId);
        }
    }
    
    async fetchPost(id) {
        try {
            const response = await window.authManager.get(`/api/posts/${id}`);
            
            if (response.ok) {
                const post = await response.json();
                this.populateForm(post);
                this.isPublished = post.published;
            } else if (response.status === 404) {
                this.showToast('Post not found', 'error');
            } else if (response.status === 403) {
                this.showToast('You can only edit your own posts', 'error');
                setTimeout(() => window.location.href = '/', 2000);
            }
        } catch (error) {
            console.error('Error loading post:', error);
            this.showToast('Failed to load post', 'error');
        }
    }
    
    populateForm(post) {
        document.getElementById('postId').value = post.id;
        document.getElementById('title').value = post.title;
        document.getElementById('slug').value = post.slug;
        document.getElementById('description').value = post.description || '';
        document.getElementById('content').value = post.content;
        
        if (post.cover) {
            document.getElementById('coverId').value = post.cover.id;
            this.updateCoverPreview(post.cover.url);
        }
        
        // Update slug preview
        const slugPreview = document.getElementById('slugPreview');
        if (slugPreview) {
            slugPreview.textContent = `/post/${post.slug}`;
        }
        
        // Update button states
        if (this.publishBtn) {
            this.publishBtn.textContent = post.published ? 'Update' : 'Publish';
        }
        if (this.saveDraftBtn) {
            this.saveDraftBtn.textContent = post.published ? 'Save Changes' : 'Save Draft';
        }
    }
    
    async savePost(publish) {
        const title = document.getElementById('title')?.value.trim();
        const slug = document.getElementById('slug')?.value.trim();
        const description = document.getElementById('description')?.value.trim();
        const content = document.getElementById('content')?.value.trim();
        const coverId = document.getElementById('coverId')?.value;
        
        // Validation
        if (!title) {
            this.showToast('Title is required', 'error');
            document.getElementById('title')?.focus();
            return;
        }
        
        if (!slug) {
            this.showToast('Slug is required', 'error');
            document.getElementById('slug')?.focus();
            return;
        }
        
        if (!content) {
            this.showToast('Content is required', 'error');
            document.getElementById('content')?.focus();
            return;
        }
        
        // Validate slug format
        const slugRegex = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
        if (!slugRegex.test(slug)) {
            this.showToast('Slug can only contain lowercase letters, numbers, and hyphens', 'error');
            return;
        }
        
        const postData = {
            title,
            slug,
            description,
            content,
            published: publish,
            coverId: coverId ? parseInt(coverId) : null
        };
        
        const isEditing = this.postId !== null;
        const url = isEditing ? `/api/posts/${this.postId}` : '/api/posts';
        const method = isEditing ? 'PUT' : 'POST';
        
        // Disable buttons
        this.setButtonsEnabled(false);
        
        try {
            const response = await window.authManager.authenticatedFetch(url, {
                method: method,
                body: JSON.stringify(postData)
            });
            
            if (response.ok) {
                const savedPost = await response.json();
                this.postId = savedPost.id;
                
                const message = publish ? 'Post published!' : 'Draft saved!';
                this.showToast(message, 'success');
                
                // Update URL if creating new post
                if (!isEditing) {
                    window.history.pushState({}, '', `/write?edit=${savedPost.id}`);
                }
                
                // Update button text
                if (publish && this.publishBtn) {
                    this.publishBtn.textContent = 'Update';
                }
                if (this.saveDraftBtn) {
                    this.saveDraftBtn.textContent = publish ? 'Save Changes' : 'Save Draft';
                }
            } else {
                const error = await response.json();
                this.showToast(error.error || 'Failed to save post', 'error');
            }
        } catch (error) {
            console.error('Save error:', error);
            this.showToast('Failed to save post', 'error');
        } finally {
            this.setButtonsEnabled(true);
        }
    }
    
    setButtonsEnabled(enabled) {
        if (this.saveDraftBtn) this.saveDraftBtn.disabled = !enabled;
        if (this.publishBtn) this.publishBtn.disabled = !enabled;
    }
    
    updateUserMenu() {
        const userMenu = document.getElementById('userMenu');
        const userAvatar = document.getElementById('userAvatar');
        
        if (this.currentUser && userAvatar) {
            const initial = this.currentUser.name.charAt(0).toUpperCase();
            userAvatar.textContent = initial;
        }
        
        if (userMenu) {
            const userMenuBtn = document.getElementById('userMenuBtn');
            const dropdown = document.querySelector('.write-header .user-menu__dropdown');
            
            if (userMenuBtn && dropdown) {
                userMenuBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    dropdown.classList.toggle('user-menu__dropdown--open');
                });
                
                document.addEventListener('click', () => {
                    dropdown.classList.remove('user-menu__dropdown--open');
                });
            }
        }
    }
    
    showToast(message, type = 'info') {
        const toast = document.getElementById('toast');
        const toastMessage = document.getElementById('toastMessage');
        
        if (!toast || !toastMessage) return;
        
        toastMessage.textContent = message;
        toast.className = `toast toast--${type}`;
        toast.style.display = 'block';
        
        setTimeout(() => {
            toast.style.display = 'none';
        }, 3000);
    }
}

// Initialize editor when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new WriteEditor();
});