package dev.vepo.contraponto.navigation;

public enum NavigationHub {
    WRITING("Writing", "/writing", "menu.writing"),
    READING("Reading", "/reading", "menu.reading"),
    MANAGE("Gerenciar", "/manage", "manage.hubTitle"),
    ACCOUNT("Account", "/account", "menu.account"),
    REVIEW("Review", "/editor", "menu.review"),
    ADMINISTRATION("Administration", "/administration", "menu.administration");

    private final String label;
    private final String path;
    private final String i18nKey;

    NavigationHub(String label, String path, String i18nKey) {
        this.label = label;
        this.path = path;
        this.i18nKey = i18nKey;
    }

    public String i18nKey() {
        return i18nKey;
    }

    public String label() {
        return label;
    }

    public String path() {
        return path;
    }
}
