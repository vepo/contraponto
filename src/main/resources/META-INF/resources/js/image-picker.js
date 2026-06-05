/**
 * User image picker: library grid in a modal plus upload-new.
 */
class ImagePickerManager {
    static MODAL_ID = 'imagePickerModal';
    static GRID_ID = 'imagePickerGrid';
    static CONTAINER_ID = 'modal-container';

    constructor() {
        this.pendingSelect = null;
        this.boundContainerClick = this.onContainerClick.bind(this);
        this.boundDocumentKeyDown = this.onDocumentKeyDown.bind(this);
        this.boundHtmxAfterSwap = this.onHtmxAfterSwap.bind(this);

        const container = document.getElementById(ImagePickerManager.CONTAINER_ID);
        if (container) {
            container.addEventListener('click', this.boundContainerClick);
        }
        document.addEventListener('keydown', this.boundDocumentKeyDown);
        document.body.addEventListener('htmx:afterSwap', this.boundHtmxAfterSwap);
    }

    open({ onSelect }) {
        if (typeof htmx === 'undefined') {
            console.warn('Image picker: htmx is not available');
            return;
        }
        this.pendingSelect = onSelect;
        htmx.ajax('GET', '/components/images/picker', {
            target: `#${ImagePickerManager.CONTAINER_ID}`,
            swap: 'innerHTML'
        });
    }

    close() {
        const container = document.getElementById(ImagePickerManager.CONTAINER_ID);
        if (container) {
            container.innerHTML = '';
        }
        this.pendingSelect = null;
        document.querySelectorAll('[data-image-picker-upload]').forEach((zone) => {
            delete zone.dataset.uploadBound;
        });
    }

    onContainerClick(event) {
        const modal = document.getElementById(ImagePickerManager.MODAL_ID);
        if (!modal) {
            return;
        }

        if (event.target.closest('[data-image-picker-close]')) {
            event.preventDefault();
            this.close();
            return;
        }

        if (event.target === modal) {
            this.close();
            return;
        }

        const item = event.target.closest('.image-picker__item[data-image-uuid][data-image-url]');
        if (item) {
            const selection = {
                id: item.dataset.imageUuid,
                url: item.dataset.imageUrl
            };
            const callback = this.pendingSelect;
            this.close();
            if (callback) {
                callback(selection);
            }
            return;
        }

        const uploadZone = event.target.closest('[data-image-picker-upload]');
        if (uploadZone && !event.target.closest('[data-image-picker-file]')) {
            uploadZone.querySelector('[data-image-picker-file]')?.click();
        }
    }

    onDocumentKeyDown(event) {
        if (event.key === 'Escape' && document.getElementById(ImagePickerManager.MODAL_ID)) {
            this.close();
        }
    }

    onHtmxAfterSwap(event) {
        const target = event.detail?.target;
        if (!target) {
            return;
        }
        if (target.id === ImagePickerManager.CONTAINER_ID
            || target.id === ImagePickerManager.MODAL_ID
            || target.querySelector?.(`#${ImagePickerManager.MODAL_ID}`)) {
            this.bindUploadZone();
        }
    }

    bindUploadZone() {
        const zone = document.querySelector('[data-image-picker-upload]');
        const fileInput = zone?.querySelector('[data-image-picker-file]');
        if (!zone || !fileInput || zone.dataset.uploadBound === 'true') {
            return;
        }
        zone.dataset.uploadBound = 'true';

        fileInput.addEventListener('change', (event) => {
            const file = event.target.files[0];
            if (file) {
                this.uploadFile(file);
            }
            fileInput.value = '';
        });

        zone.addEventListener('dragover', (event) => {
            event.preventDefault();
            zone.classList.add('drag-over');
        });
        zone.addEventListener('dragleave', () => {
            zone.classList.remove('drag-over');
        });
        zone.addEventListener('drop', (event) => {
            event.preventDefault();
            zone.classList.remove('drag-over');
            const file = event.dataTransfer.files[0];
            if (file && file.type.startsWith('image/')) {
                this.uploadFile(file);
            }
        });
    }

    async uploadFile(file) {
        const formData = new FormData();
        formData.append('file', file);
        const csrf = document.querySelector('meta[name="csrf-token"]')?.content;

        try {
            const response = await fetch('/api/images', {
                method: 'POST',
                headers: csrf ? { 'X-CSRF-Token': csrf } : {},
                body: formData
            });
            if (!response.ok) {
                throw new Error('Upload failed');
            }
            const image = await response.json();
            const callback = this.pendingSelect;
            this.close();
            if (callback) {
                callback({ id: image.id, url: image.url });
            }
        } catch (err) {
            console.error(err);
            const translated = window.i18n?.t('toast.image.uploadFailed');
            const message = translated && translated !== 'toast.image.uploadFailed'
                ? translated
                : 'Falha ao enviar a imagem. Tente novamente.';
            if (window.toastManager?.show) {
                window.toastManager.show(message, 'error');
            } else {
                alert(message);
            }
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.imagePicker = new ImagePickerManager();
});
