class MainManager {
    constructor() {
        this.setupRouteBasedElementsEnabler();
    }

    setupRouteBasedElementsEnabler() {
        document.body.addEventListener('htmx:afterSwap', evt => {
            document.querySelectorAll("[data-disable-pattern]")
                .forEach(elm => {
                    const pattern = new RegExp(elm.dataset.disablePattern);
                    if (pattern.test(window.location.pathname)) {
                        elm.classList.add('disabled');
                    } else {
                        elm.classList.remove('disabled');
                    }
                })
        })
    }
}
document.addEventListener('DOMContentLoaded', () => {
    new MainManager();
});