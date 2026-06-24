package dev.vepo.contraponto.blog;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class BlogSubdomainTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("app.blog-subdomain.enabled", "true",
                      "app.blog-subdomain.base-domain", "localhost",
                      "app.platform.host", "blogs.localhost",
                      "image.base.url", "https://blogs.localhost");
    }
}
