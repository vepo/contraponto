class ContrapontoAuthentication {
    constructor() {
        const _this = this;
        document.body.addEventListener('htmx:afterOnLoad', (evt) =>{
            console.log('htmx:afterOnLoad', evt);
        });
        document.body.addEventListener('htmx:configRequest', (evt) => {
            console.debug('Authentication start', evt, _this);
            const token = _this.loadToken();
            if (token) {
                evt.detail.headers['Authorization'] = `Bearer ${token}`;
            }
        });

        document.body.addEventListener('htmx:xhr:loadend', evt => {
            console.log('HTTP Response', evt);
        });

        const token = _this.loadToken();
        if (token) {
            htmx.trigger("body", "loggedIn");
        }
    }

    loadToken() {
        let token = localStorage.getItem("__contraponto_token");
        if (token) {
            return JSON.parse(token);
        }
        return null;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new ContrapontoAuthentication();
});