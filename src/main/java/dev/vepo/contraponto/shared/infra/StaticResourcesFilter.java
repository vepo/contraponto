package dev.vepo.contraponto.shared.infra;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class StaticResourcesFilter implements ContainerRequestFilter {

    private static class CachedResource {
        final byte[] content;
        final MediaType mediaType;
        final String etag;
        final String lastModified;

        CachedResource(byte[] content, MediaType mediaType, String etag, String lastModified) {
            this.content = content;
            this.mediaType = mediaType;
            this.etag = etag;
            this.lastModified = lastModified;
        }
    }

    // Cache of static files (never changes at runtime)
    private static final ConcurrentMap<String, CachedResource> CACHE = new ConcurrentHashMap<>();
    private static final String STATIC_ROOT = "/META-INF/resources/";
    private static final long ONE_MONTH_SECONDS = 2_592_000L; // 30 days

    private static final DateTimeFormatter HTTP_DATE_FORMAT =
            DateTimeFormatter.RFC_1123_DATE_TIME;

    private String computeETag(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return '"' + sb.toString() + '"';
        } catch (NoSuchAlgorithmException e) {
            // Fallback: use length as weak etag
            return '"' + String.valueOf(content.length) + '"';
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Only intercept GET or HEAD requests
        String method = requestContext.getMethod();
        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();
        if (path == null || path.isEmpty() || path.isBlank() || path.equals("/")) {
            return;
        }

        // Build resource path: META-INF/resources/<path>
        String resourcePath = STATIC_ROOT + path;
        CachedResource cached = CACHE.get(path);

        if (cached == null) {
            // Try to load it from classpath
            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                if (in == null) {
                    // Resource not found – let the request continue normally
                    return;
                }

                // Read the content into a byte array
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[8192];
                int n;
                while ((n = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, n);
                }
                byte[] content = buffer.toByteArray();

                // Determine content type
                MediaType mediaType = guessMediaType(path);

                // Compute ETag from SHA-1 of the content
                String etag = computeETag(content);

                // Last-Modified: we can't know the real file modification time
                // inside a JAR, so we use the application build time or a fixed date.
                // A fixed date like "Thu, 01 Jan 2025 00:00:00 GMT" is harmless.
                String lastModified = ZonedDateTime.now(ZoneOffset.UTC).format(HTTP_DATE_FORMAT);

                cached = new CachedResource(content, mediaType, etag, lastModified);
                CACHE.put(path, cached);
            }
        }

        // Build cache-control header
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge((int) ONE_MONTH_SECONDS);
        cacheControl.setPrivate(false);

        Response.ResponseBuilder responseBuilder = Response.ok(cached.content, cached.mediaType.toString())
                                                           .cacheControl(cacheControl)
                                                           .header("ETag", cached.etag)
                                                           .header("Last-Modified", cached.lastModified);

        // Handle conditional requests
        String ifNoneMatch = requestContext.getHeaderString("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.equals(cached.etag)) {
            responseBuilder.status(Response.Status.NOT_MODIFIED);
            responseBuilder.entity(null); // no body for 304
        }

        requestContext.abortWith(responseBuilder.build());
    }

    private MediaType guessMediaType(String path) {
        if (path.endsWith(".ico")) {
            return new MediaType("image", "x-icon");
        } else if (path.endsWith(".png")) {
            return new MediaType("image", "png");
        } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
            return new MediaType("image", "jpeg");
        } else if (path.endsWith(".gif")) {
            return new MediaType("image", "gif");
        } else if (path.endsWith(".css")) {
            return new MediaType("text", "css");
        } else if (path.endsWith(".js")) {
            return new MediaType("application", "javascript");
        } else {
            // fallback
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        }
    }
}