class ContrapontoAuthentication {
    constructor() {
        document.body.addEventListener('htmx:configRequest', (evt) => {
            const token = this.loadToken();
            if (token) {
                evt.detail.headers['Authorization'] = `Bearer ${token}`;
            }
        });
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
