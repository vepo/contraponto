package dev.vepo.contraponto.shared.smoke;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.shared.TestTags;

public final class DockerSmokeStackExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger logger = LoggerFactory.getLogger(DockerSmokeStackExtension.class);

    private static final Path COMPOSE_DIR = Path.of("src/test/resources/docker-smoke");

    private static volatile boolean started;

    private static void bindOriginsFromCompose() {
        DockerSmokeUrls.bindPlatformOrigin("http://%s:%d".formatted(DockerSmokeUrls.PLATFORM_HOST, DockerSmokeUrls.NGINX_HOST_PORT));
        logger.info("Platform origin {} (nginx on host port {})", DockerSmokeUrls.platform(), DockerSmokeUrls.NGINX_HOST_PORT);
    }

    private static boolean healthCheck(String url, String hostHeader) {
        try {
            var builder = java.net.http.HttpRequest.newBuilder().uri(java.net.URI.create(url)).GET();
            if (hostHeader != null) {
                builder.header("Host", hostHeader);
            }
            var response = java.net.http.HttpClient.newHttpClient()
                                                   .send(builder.build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            logger.debug("Health check failed for {} (Host={})", url, hostHeader, e);
            return false;
        }
    }

    private static int postgresHostPort() {
        try {
            var output = runCompose("port", "postgres", "5432");
            var line = output.trim();
            var colon = line.lastIndexOf(':');
            if (colon < 0) {
                throw new IllegalStateException("Unexpected postgres port output: %s".formatted(line));
            }
            return Integer.parseInt(line.substring(colon + 1));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to resolve postgres host port", e);
        }
    }

    private static void resetAdminPassword() {
        var jdbcUrl = "jdbc:postgresql://127.0.0.1:%d/contraponto_db".formatted(postgresHostPort());
        var hash = BCrypt.hashpw(SmokeCredentials.ADMIN_PASSWORD, BCrypt.gensalt());
        try (var connection = DriverManager.getConnection(jdbcUrl,
                                                          "contraponto_user",
                                                          SmokeCredentials.POSTGRES_PASSWORD);
                var statement = connection.prepareStatement("UPDATE tb_users SET password_hash = ? WHERE username = ?")) {
            statement.setString(1, hash);
            statement.setString(2, SmokeCredentials.ADMIN_USERNAME);
            var updated = statement.executeUpdate();
            if (updated != 1) {
                throw new IllegalStateException("Expected to update admin password, rows=%d".formatted(updated));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reset admin password for smoke tests", e);
        }
    }

    private static String runCompose(String... args) throws IOException, InterruptedException {
        var image = System.getProperty("contraponto.smoke.image", "contraponto:ci-smoke");
        var command = new ArrayList<String>();
        command.add("docker");
        command.add("compose");
        command.addAll(java.util.Arrays.asList(args));
        var processBuilder = new ProcessBuilder(command)
                                                        .directory(COMPOSE_DIR.toFile())
                                                        .redirectErrorStream(true);
        var environment = processBuilder.environment();
        environment.put("CONTRAPONTO_IMAGE", image);
        environment.put("POSTGRES_PASSWORD", SmokeCredentials.POSTGRES_PASSWORD);
        environment.put("PASSWORD_SALT", SmokeCredentials.PASSWORD_SALT);
        var startedProcess = processBuilder.start();
        var output = new String(startedProcess.getInputStream().readAllBytes());
        var exit = startedProcess.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("docker compose %s failed (exit %d): %s".formatted(String.join(" ", args), exit, output));
        }
        logger.info("docker compose {} -> {}", String.join(" ", args), output.trim());
        return output;
    }

    private static void startStack() {
        var image = System.getProperty("contraponto.smoke.image", "contraponto:ci-smoke");
        logger.info("Starting docker-smoke stack with image {}", image);
        try {
            runCompose("up", "-d", "--wait");
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to start docker-smoke stack", e);
        }
        bindOriginsFromCompose();
        waitForPlatformHealth();
        resetAdminPassword();
        logger.info("Docker-smoke stack ready at {}", DockerSmokeUrls.platform());
    }

    private static void stopStack() {
        try {
            runCompose("down", "-v");
        } catch (IOException | InterruptedException e) {
            logger.warn("Failed to tear down docker-smoke stack", e);
        }
    }

    private static void waitForPlatformHealth() {
        Awaitility.await()
                  .atMost(Duration.ofMinutes(2))
                  .pollInterval(Duration.ofSeconds(3))
                  .until(() -> healthCheck("http://127.0.0.1:%d/q/health".formatted(DockerSmokeUrls.NGINX_HOST_PORT), null));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (!context.getTags().contains(TestTags.DOCKER_SMOKE) || !started) {
            return;
        }
        stopStack();
        started = false;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!context.getTags().contains(TestTags.DOCKER_SMOKE)) {
            return;
        }
        if (started) {
            return;
        }
        synchronized (DockerSmokeStackExtension.class) {
            if (started) {
                return;
            }
            startStack();
            started = true;
        }
    }
}
