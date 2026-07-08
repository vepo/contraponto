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

    openPicker(onSelect) {
        if (window.imagePicker) {
            window.imagePicker.open({ onSelect });
            return;
        }
        console.warn('Image picker is not available');
    }

    async uploadImage(file, onSuccess) {
        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch('/api/images', {
                method: 'POST',
                headers: {
                    'X-CSRF-Token': document.querySelector('meta[name="csrf-token"]').content
                },
                body: formData
            });
            if (!response.ok) {
                throw new Error('Upload failed');
            }
            const image = await response.json();
            onSuccess(image.id, image.url);
        } catch (err) {
            console.error(err);
            alert('Failed to upload image. Please try again.');
        }
    }

    setupCoverUpload() {
        const coverArea = document.getElementById('coverUploadArea');
        const coverInput = document.getElementById('coverInput');
        const coverPlaceholder = document.getElementById('coverPlaceholder');
        const coverPreview = document.getElementById('coverPreview');
        const removeBtn = document.getElementById('removeCoverBtn');

        if (!coverArea || coverArea.dataset.coverBound === 'true') {
            return;
        }
        coverArea.dataset.coverBound = 'true';

        coverInput?.addEventListener('change', (event) => {
            const file = event.target.files[0];
            if (file) {
                this.uploadImage(file, (id, url) => {
                    this.setCover(id, url);
                });
            }
        });

        coverArea.addEventListener('click', (event) => {
            if (event.target === removeBtn || removeBtn?.contains(event.target)) {
                return;
            }
            this.openPicker((image) => {
                this.setCover(image.id, image.url);
            });
        });

        removeBtn?.addEventListener('click', (event) => {
            event.stopPropagation();
            document.getElementById('coverId').value = '';
            coverPreview.classList.add('u-hidden');
            coverPlaceholder.classList.remove('u-hidden');
        });
    }

    setupImageUploadAreas() {
        document.querySelectorAll('[data-image-upload]:not([data-upload-bound])').forEach((area) => {
            area.setAttribute('data-upload-bound', 'true');
            const hiddenFieldId = area.dataset.hiddenField;
            const hiddenField = document.getElementById(hiddenFieldId);
            const placeholder = area.querySelector('[data-upload-placeholder]');
            const preview = area.querySelector('[data-upload-preview]');
            const previewImg = area.querySelector('[data-upload-preview-img]');
            const removeBtn = area.querySelector('[data-upload-remove]');

            if (!hiddenField) {
                return;
            }

            area.addEventListener('click', (event) => {
                if (event.target === removeBtn || removeBtn?.contains(event.target)) {
                    return;
                }
                this.openPicker((image) => {
                    hiddenField.value = image.id;
                    previewImg.src = image.url;
                    placeholder.classList.add('u-hidden');
                    preview.classList.remove('u-hidden');
                });
            });

            removeBtn?.addEventListener('click', (event) => {
                event.stopPropagation();
                hiddenField.value = '';
                preview.classList.add('u-hidden');
                placeholder.classList.remove('u-hidden');
            });
        });
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

function initImageUploader() {
    if (!window.imageUploader) {
        window.imageUploader = new ImageUploader();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    if (document.getElementById('coverUploadArea') || document.querySelector('[data-image-upload]')) {
        initImageUploader();
    }
});

document.addEventListener('contraponto:assets-ready', (event) => {
    if (event.detail?.profile === 'write') {
        initImageUploader();
    }
});
