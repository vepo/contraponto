package dev.vepo.contraponto.shared.infra;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CurrentPageAssets {

    private PageAssets profile = PageAssets.PUBLIC_READ;

    public PageAssets get() {
        return profile;
    }

    public void set(PageAssets profile) {
        this.profile = profile != null ? profile : PageAssets.PUBLIC_READ;
    }
}
