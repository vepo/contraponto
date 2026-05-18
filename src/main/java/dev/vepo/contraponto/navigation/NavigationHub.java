package dev.vepo.contraponto.navigation;

public enum NavigationHub {
    WRITING("Writing", "/writing"),
    MANAGE("Manage", "/manage"),
    ACCOUNT("Account", "/account"),
    REVIEW("Review", "/editor"),
    ADMINISTRATION("Administration", "/administration");

    private final String label;
    private final String path;

    NavigationHub(String label, String path) {
        this.label = label;
        this.path = path;
    }

    public String label() {
        return label;
    }

    public String path() {
        return path;
    }
}
