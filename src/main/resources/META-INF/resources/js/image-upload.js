class ImageUploader {
    constructor() {
        this.setupCoverUpload = this.setupCoverUpload.bind(this);
        this.setupImageUploadAreas = this.setupImageUploadAreas.bind(this);
        this.setupCoverUpload();
        this.setupImageUploadAreas();
        document.body.addEventListener('htmx:afterSwap', () => {
            this.setupCoverUpload();
            this.setupImageUploadAreas();
        });
    }

    setupCoverUpload() {
        const coverArea = document.getElementById('coverUploadArea');
        const coverInput = document.getElementById('coverInput');
        const coverPlaceholder = document.getElementById('coverPlaceholder');
        const coverPreview = document.getElementById('coverPreview');
        const coverPreviewImg = document.getElementById('coverPreviewImg');
        const coverIdField = document.getElementById('coverId');
        const removeBtn = document.getElementById('removeCoverBtn');

        if (!coverArea) return;

        coverArea.addEventListener('click', (e) => {
            if (e.target === removeBtn || removeBtn?.contains(e.target)) return;
            coverInput.click();
        });

        coverArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            coverArea.classList.add('drag-over');
        });
        coverArea.addEventListener('dragleave', () => {
            coverArea.classList.remove('drag-over');
        });
        coverArea.addEventListener('drop', (e) => {
            e.preventDefault();
            coverArea.classList.remove('drag-over');
            const file = e.dataTransfer.files[0];
            if (file && file.type.startsWith('image/')) {
                this.uploadImage(file, document.querySelector('[name="blogId"]')?.value, (id, url) => {
                    this.setCover(id, url);
                });
            }
        });

        coverInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                this.uploadImage(file, document.querySelector('[name="blogId"]')?.value, (id, url) => {
                    this.setCover(id, url);
                });
            }
        });

        removeBtn?.addEventListener('click', (e) => {
            e.stopPropagation();
            coverIdField.value = '';
            coverPreview.classList.add('u-hidden');
            coverPlaceholder.classList.remove('u-hidden');
            coverInput.value = '';
        });
    }

    setupImageUploadAreas() {
        document.querySelectorAll('[data-image-upload]:not([data-upload-bound])').forEach((area) => {
            area.setAttribute('data-upload-bound', 'true');
            const hiddenFieldId = area.dataset.hiddenField;
            const blogId = area.dataset.blogId;
            const hiddenField = document.getElementById(hiddenFieldId);
            const fileInput = area.querySelector('[data-upload-input]');
            const placeholder = area.querySelector('[data-upload-placeholder]');
            const preview = area.querySelector('[data-upload-preview]');
            const previewImg = area.querySelector('[data-upload-preview-img]');
            const removeBtn = area.querySelector('[data-upload-remove]');

            if (!hiddenField || !fileInput) return;

            area.addEventListener('click', (e) => {
                if (e.target === removeBtn || removeBtn?.contains(e.target)) return;
                fileInput.click();
            });

            area.addEventListener('dragover', (e) => {
                e.preventDefault();
                area.classList.add('drag-over');
            });
            area.addEventListener('dragleave', () => {
                area.classList.remove('drag-over');
            });
            area.addEventListener('drop', (e) => {
                e.preventDefault();
                area.classList.remove('drag-over');
                const file = e.dataTransfer.files[0];
                if (file && file.type.startsWith('image/')) {
                    this.uploadImage(file, blogId, (id, url) => {
                        hiddenField.value = id;
                        previewImg.src = url;
                        placeholder.classList.add('u-hidden');
                        preview.classList.remove('u-hidden');
                    });
                }
            });

            fileInput.addEventListener('change', (e) => {
                const file = e.target.files[0];
                if (file) {
                    this.uploadImage(file, blogId, (id, url) => {
                        hiddenField.value = id;
                        previewImg.src = url;
                        placeholder.classList.add('u-hidden');
                        preview.classList.remove('u-hidden');
                    });
                }
            });

            removeBtn?.addEventListener('click', (e) => {
                e.stopPropagation();
                hiddenField.value = '';
                preview.classList.add('u-hidden');
                placeholder.classList.remove('u-hidden');
                fileInput.value = '';
            });
        });
    }

    async uploadImage(file, blogId, onSuccess) {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch(`/api/images?blogId=${encodeURIComponent(blogId)}`, {
                method: 'POST',
                headers: {
                    'X-CSRF-Token': document.querySelector('meta[name="csrf-token"]').content
                },
                body: formData
            });
            if (!response.ok) throw new Error('Upload failed');
            const image = await response.json();
            onSuccess(image.id, image.url);
        } catch (err) {
            console.error(err);
            alert('Failed to upload image. Please try again.');
        }
    }

    setCover(id, url) {
        const coverIdField = document.getElementById('coverId');
        const coverPlaceholder = document.getElementById('coverPlaceholder');
        const coverPreview = document.getElementById('coverPreview');
        const coverPreviewImg = document.getElementById('coverPreviewImg');

        coverIdField.value = id;
        coverPreviewImg.src = url;
        coverPlaceholder.classList.add('u-hidden');
        coverPreview.classList.remove('u-hidden');
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new ImageUploader();
});
