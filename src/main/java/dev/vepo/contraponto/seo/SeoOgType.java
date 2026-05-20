package dev.vepo.contraponto.seo;

public enum SeoOgType {
    WEBSITE("website"),
    ARTICLE("article"),
    PROFILE("profile");

    private final String value;

    SeoOgType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
