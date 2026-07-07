package dev.vepo.contraponto.activitypub;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebFingerLink(String rel, String type, String href, String template) {

    public static WebFingerLink hrefLink(String rel, String type, String href) {
        return new WebFingerLink(rel, type, href, null);
    }

    public static WebFingerLink templateLink(String rel, String template) {
        return new WebFingerLink(rel, null, null, template);
    }
}
