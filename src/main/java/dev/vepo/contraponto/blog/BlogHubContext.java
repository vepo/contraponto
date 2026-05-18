package dev.vepo.contraponto.blog;

public enum BlogHubContext {
    WRITING,
    MANAGE;

    public static BlogHubContext fromHubParam(String hub) {
        if ("manage".equalsIgnoreCase(hub)) {
            return MANAGE;
        }
        return WRITING;
    }

    public boolean authorMode() {
        return this == WRITING;
    }

    public String hubParam() {
        return name().toLowerCase();
    }
}
