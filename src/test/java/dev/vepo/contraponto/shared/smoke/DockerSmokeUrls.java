package dev.vepo.contraponto.shared.smoke;

/**
 * Platform and author-subdomain origins for docker-smoke (mirrors
 * commit-mestre.prod naming).
 */
public final class DockerSmokeUrls {

    public static final String BASE_DOMAIN = "commit-mestre.test";

    public static final String PLATFORM_HOST = "blogs." + BASE_DOMAIN;

    public static final int NGINX_HOST_PORT = 8080;

    private static volatile String platformOrigin = "http://" + PLATFORM_HOST + ":" + NGINX_HOST_PORT;

    public static String authorOrigin(String username) {
        var platform = platformOrigin.replace("http://", "").replace("https://", "");
        var portSuffix = "";
        var hostOnly = platform;
        var colon = platform.indexOf(':');
        if (colon >= 0) {
            hostOnly = platform.substring(0, colon);
            portSuffix = platform.substring(colon);
        }
        var authorHost = hostOnly.replace("blogs.", username + ".");
        return "http://" + authorHost + portSuffix;
    }

    public static void bindPlatformOrigin(String origin) {
        platformOrigin = origin;
    }

    public static String platform() {
        return platformOrigin;
    }

    private DockerSmokeUrls() {}
}
