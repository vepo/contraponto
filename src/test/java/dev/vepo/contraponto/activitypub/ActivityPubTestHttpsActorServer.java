package dev.vepo.contraponto.activitypub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

final class ActivityPubTestHttpsActorServer implements AutoCloseable {

    private static final String KEYSTORE_RESOURCE = "/activitypub-test-https.p12";
    private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();

    private static SSLContext loadSslContext() throws Exception {
        try (InputStream input = ActivityPubTestHttpsActorServer.class.getResourceAsStream(KEYSTORE_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing test keystore " + KEYSTORE_RESOURCE);
            }
            var keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(input, KEYSTORE_PASSWORD);
            var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD);
            var sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
            return sslContext;
        }
    }

    static ActivityPubTestHttpsActorServer start(String actorPath, String responseBody) throws Exception {
        var sslContext = loadSslContext();
        var server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var actorServer = new ActivityPubTestHttpsActorServer(server, server.getAddress().getPort());
        actorServer.responseBody.set(responseBody);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                params.setNeedClientAuth(false);
                params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
            }
        });
        server.createContext(actorPath, actorServer::writeResponse);
        server.start();
        return actorServer;
    }

    private final HttpsServer server;

    private final int port;

    private final AtomicReference<String> responseBody = new AtomicReference<>("");

    private ActivityPubTestHttpsActorServer(HttpsServer server, int port) {
        this.server = server;
        this.port = port;
    }

    String actorUrl(String actorPath) {
        return "https://127.0.0.1:%d%s".formatted(port, actorPath);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    int port() {
        return port;
    }

    void updateResponse(String body) {
        responseBody.set(body);
    }

    private void writeResponse(HttpExchange exchange) throws IOException {
        var body = responseBody.get();
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", ActivityPubPaths.ACTIVITY_JSON);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
