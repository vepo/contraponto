class ImageUploader {
    constructor() {
        this.setupCoverUpload = this.setupCoverUpload.bind(this);
        this.setupCoverUpload();
        document.body.addEventListener('htmx:afterSwap', this.setupCoverUpload);
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

        // Click to open file picker
        coverArea.addEventListener('click', (e) => {
            if (e.target === removeBtn || removeBtn.contains(e.target)) return;
            coverInput.click();
        });

        // Drag & drop
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
                this.uploadImage(file);
            }
        });

        // File input change
        coverInput.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) this.uploadImage(file);
        });

        // Remove cover
        removeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            coverIdField.value = '';
            coverPreview.style.display = 'none';
            coverPlaceholder.style.display = 'flex';
            coverInput.value = '';
        });
    }

    async uploadImage(file) {
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
            if (!response.ok) throw new Error('Upload failed');
            const image = await response.json();
            this.setCover(image.id, image.url);
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
        coverPlaceholder.style.display = 'none';
        coverPreview.style.display = 'block';
    }
}

// Initialize when DOM ready
document.addEventListener('DOMContentLoaded', () => {
    new ImageUploader();
});