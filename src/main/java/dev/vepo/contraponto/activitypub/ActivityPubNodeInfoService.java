package dev.vepo.contraponto.activitypub;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubNodeInfoService {

    private final BlogSubdomainConfig subdomainConfig;

    @Inject
    public ActivityPubNodeInfoService(BlogSubdomainConfig subdomainConfig) {
        this.subdomainConfig = subdomainConfig;
    }

    public Map<String, Object> buildNodeInfo20Document() {
        var document = new LinkedHashMap<String, Object>();
        document.put("version", "2.0");
        document.put("software", Map.of("name", "contraponto", "version", "0.0.1"));
        document.put("protocols", List.of("activitypub"));
        document.put("services", Map.of("inbound", List.of(), "outbound", List.of()));
        document.put("openRegistrations", false);
        document.put("usage", Map.of("users", Map.of("total", 0, "activeMonth", 0, "activeHalfyear", 0)));
        document.put("metadata", Map.of());
        return document;
    }

    public WebFingerJrd buildWellKnownDocument() {
        var nodeInfoUrl = subdomainConfig.platformUrl("/nodeinfo/2.0");
        return new WebFingerJrd(null,
                                null,
                                List.of(new WebFingerLink("http://nodeinfo.diaspora.software/ns/schema/1.0",
                                                          "application/json",
                                                          nodeInfoUrl,
                                                          null)));
    }
}
