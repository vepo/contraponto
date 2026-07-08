package dev.vepo.contraponto.activitypub;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.BlogSubdomainContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubHttpSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPubHttpSignatureService.class);

    /**
     * HTTP-date (RFC 7231 / RFC 1123). Must zero-pad the day-of-month: Mastodon
     * parses {@code Date} with Ruby {@code Time.httpdate}, which rejects
     * {@code Wed, 8 Jul …} (Java {@link DateTimeFormatter#RFC_1123_DATE_TIME} emits
     * a single digit) and returns HTTP 401 on signature verify.
     */
    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
                                                                        .withLocale(Locale.US)
                                                                        .withZone(ZoneOffset.UTC);

    private static final Pattern SIGNATURE_PARAM = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    static String formatHttpDate(ZonedDateTime instant) {
        return HTTP_DATE.format(instant);
    }

    private final ActivityPubKeyPairService keyPairService;
    private final ActivityPubActorRepository actorRepository;
    private final ActivityPubRemoteActorRepository remoteActorRepository;
    private final ActivityPubRemoteActorService remoteActorService;
    private final ActivityPubFetchSettings fetchSettings;

    private final BlogSubdomainContext subdomainContext;

    @Inject
    public ActivityPubHttpSignatureService(ActivityPubKeyPairService keyPairService,
                                           ActivityPubActorRepository actorRepository,
                                           ActivityPubRemoteActorRepository remoteActorRepository,
                                           ActivityPubRemoteActorService remoteActorService,
                                           ActivityPubFetchSettings fetchSettings,
                                           BlogSubdomainContext subdomainContext) {
        this.keyPairService = keyPairService;
        this.actorRepository = actorRepository;
        this.remoteActorRepository = remoteActorRepository;
        this.remoteActorService = remoteActorService;
        this.fetchSettings = fetchSettings;
        this.subdomainContext = subdomainContext;
    }

    private Map<String, String> buildHeaderMapForVerification(String method,
                                                              URI requestUri,
                                                              String body,
                                                              Map<String, String> headers) {
        var map = new LinkedHashMap<String, String>();
        map.put("(request-target)", buildRequestTarget(method, requestUri));
        for (var entry : headers.entrySet()) {
            map.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
        }
        if (!map.containsKey("digest")) {
            map.put("digest", computeDigest(body));
        }
        return map;
    }

    private String buildRequestTarget(String method, URI uri) {
        var path = subdomainContext.signatureRequestPath().orElseGet(() -> requestPathFromUri(uri));
        return "%s %s".formatted(method.toLowerCase(Locale.ROOT), path);
    }

    private String buildSigningString(Map<String, String> orderedValues) {
        var sb = new StringBuilder();
        var first = true;
        for (var entry : orderedValues.entrySet()) {
            if (!first) {
                sb.append('\n');
            }
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }

    public String computeDigest(String body) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
            return "SHA-256=%s".formatted(Base64.getEncoder().encodeToString(hash));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private Optional<String> findMissingSignedHeader(String signedHeaders, Map<String, String> headerMap) {
        for (var name : signedHeaders.split("\\s+")) {
            var key = name.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                continue;
            }
            if (!headerMap.containsKey(key)) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }

    private boolean hasFreshRemoteKey(ActivityPubRemoteActor remote) {
        if (remote.getPublicKeyPem() == null || remote.getPublicKeyPem().isBlank()) {
            return false;
        }
        var fetchedAt = remote.getProfileFetchedAt();
        if (fetchedAt == null) {
            return false;
        }
        return fetchedAt.isAfter(LocalDateTime.now().minusDays(fetchSettings.profileMaxAgeDays()));
    }

    private String headerValue(Map<String, String> headers, String name) {
        for (var entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Optional<PublicKey> parseRemotePublicKey(ActivityPubRemoteActor remote) {
        if (remote.getPublicKeyPem() == null || remote.getPublicKeyPem().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(keyPairService.parsePublicKeyPem(remote.getPublicKeyPem()));
    }

    private Map<String, String> parseSignatureHeader(String header) {
        var params = new LinkedHashMap<String, String>();
        Matcher matcher = SIGNATURE_PARAM.matcher(header);
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }
        return params;
    }

    private String requestPathFromUri(URI uri) {
        var path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        var query = uri.getRawQuery();
        if (query != null && !query.isBlank()) {
            path = "%s?%s".formatted(path, query);
        }
        return path;
    }

    private Optional<PublicKey> resolvePublicKey(String keyId) {
        var actorUrl = ActivityPubActorUrls.actorUrlFromKeyId(keyId);
        var cached = remoteActorRepository.findByPublicKeyId(keyId)
                                          .or(() -> remoteActorRepository.findByActorId(actorUrl));
        if (cached.isPresent() && hasFreshRemoteKey(cached.get())) {
            return parseRemotePublicKey(cached.get());
        }
        var local = actorRepository.findByPublicKeyId(keyId);
        if (local.isPresent()) {
            return Optional.of(keyPairService.parsePublicKeyPem(local.get().getPublicKeyPem()));
        }
        if (cached.isPresent() && cached.get().getPublicKeyPem() != null) {
            return parseRemotePublicKey(cached.get());
        }
        remoteActorService.fetchAndCache(actorUrl);
        return resolvePublicKeyFromCache(keyId);
    }

    private Optional<PublicKey> resolvePublicKeyFromCache(String keyId) {
        var local = actorRepository.findByPublicKeyId(keyId);
        if (local.isPresent()) {
            return Optional.of(keyPairService.parsePublicKeyPem(local.get().getPublicKeyPem()));
        }
        var actorUrl = ActivityPubActorUrls.actorUrlFromKeyId(keyId);
        var remote = remoteActorRepository.findByPublicKeyId(keyId)
                                          .or(() -> remoteActorRepository.findByActorId(actorUrl));
        if (remote.isEmpty()) {
            return Optional.empty();
        }
        return parseRemotePublicKey(remote.get());
    }

    private String sign(String signingString, PrivateKey privateKey) {
        try {
            var signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signingString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to sign request", ex);
        }
    }

    private String signingStringFromHeaderList(String signedHeaders, Map<String, String> headerMap) {
        var sb = new StringBuilder();
        var first = true;
        for (var name : signedHeaders.split("\\s+")) {
            var key = name.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                continue;
            }
            var value = headerMap.get(key);
            if (value == null) {
                throw new IllegalStateException("missing signed header: " + key);
            }
            if (!first) {
                sb.append('\n');
            }
            sb.append(key).append(": ").append(value);
            first = false;
        }
        return sb.toString();
    }

    public Map<String, String> signRequest(PrivateKey privateKey,
                                           String keyId,
                                           String method,
                                           URI targetUri,
                                           String body) {
        var date = formatHttpDate(ZonedDateTime.now(ZoneOffset.UTC));
        var digest = computeDigest(body);
        var host = targetUri.getHost();
        if (targetUri.getPort() > 0 && targetUri.getPort() != 443 && targetUri.getPort() != 80) {
            host = "%s:%d".formatted(host, targetUri.getPort());
        }
        var requestTarget = buildRequestTarget(method, targetUri);
        var signingValues = new LinkedHashMap<String, String>();
        signingValues.put("(request-target)", requestTarget);
        signingValues.put("host", host);
        signingValues.put("date", date);
        signingValues.put("digest", digest);
        var signingString = buildSigningString(signingValues);
        var signatureValue = sign(signingString, privateKey);
        var signatureHeader = "keyId=\"%s\",algorithm=\"rsa-sha256\",headers=\"(request-target) host date digest\",signature=\"%s\""
                                                                                                                                    .formatted(keyId,
                                                                                                                                               signatureValue);
        var headers = new LinkedHashMap<String, String>();
        headers.put("Date", date);
        headers.put("Digest", digest);
        headers.put("Host", host);
        headers.put("Signature", signatureHeader);
        return headers;
    }

    private boolean verify(String signingString, String signatureValue, PublicKey publicKey) {
        try {
            var signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(signingString.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureValue));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            logger.debug("Signature verification failed", ex);
            return false;
        }
    }

    public boolean verifyRequest(String method,
                                 URI requestUri,
                                 String body,
                                 Map<String, String> headers) {
        var signatureHeader = headerValue(headers, "Signature");
        if (signatureHeader == null || signatureHeader.isBlank()) {
            logger.warn("ActivityPub inbox signature missing Signature header");
            return false;
        }
        var params = parseSignatureHeader(signatureHeader);
        var keyId = params.get("keyId");
        var algorithm = params.get("algorithm");
        var signedHeaders = params.get("headers");
        var signature = params.get("signature");
        if (keyId == null || algorithm == null || signedHeaders == null || signature == null) {
            logger.warn("ActivityPub inbox signature incomplete params keyId={} params={}", keyId, params.keySet());
            return false;
        }
        if (!"rsa-sha256".equalsIgnoreCase(algorithm)) {
            logger.warn("ActivityPub inbox signature unsupported algorithm keyId={} algorithm={}", keyId, algorithm);
            return false;
        }
        var publicKey = resolvePublicKey(keyId);
        if (publicKey.isEmpty()) {
            logger.warn("ActivityPub inbox signature no public key for keyId={}", keyId);
            return false;
        }
        var verified = verifySignedHeaders(method, requestUri, body, headers, signedHeaders, signature, publicKey.get(), keyId);
        if (verified) {
            return true;
        }
        var actorUrl = ActivityPubActorUrls.actorUrlFromKeyId(keyId);
        remoteActorService.fetchAndCache(actorUrl, true);
        var retryKey = resolvePublicKeyFromCache(keyId);
        if (retryKey.isEmpty()) {
            logger.warn("ActivityPub inbox signature no public key after actor refetch keyId={}", keyId);
            return false;
        }
        return verifySignedHeaders(method, requestUri, body, headers, signedHeaders, signature, retryKey.get(), keyId);
    }

    public boolean verifyRequest(String method,
                                 URI requestUri,
                                 String body,
                                 Map<String, String> headers,
                                 PublicKey publicKey) {
        var signatureHeader = headerValue(headers, "Signature");
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        var params = parseSignatureHeader(signatureHeader);
        var signedHeaders = params.get("headers");
        var signature = params.get("signature");
        if (signedHeaders == null || signature == null) {
            return false;
        }
        return verifySignedHeaders(method, requestUri, body, headers, signedHeaders, signature, publicKey, null);
    }

    private boolean verifySignedHeaders(String method,
                                        URI requestUri,
                                        String body,
                                        Map<String, String> headers,
                                        String signedHeaders,
                                        String signature,
                                        PublicKey publicKey,
                                        String keyId) {
        var headerMap = buildHeaderMapForVerification(method, requestUri, body, headers);
        var requestTarget = buildRequestTarget(method, requestUri);
        var host = headerValue(headers, "Host");
        var declaredDigest = headerMap.get("digest");
        if (declaredDigest == null) {
            logger.warn("ActivityPub inbox signature missing Digest header keyId={} requestTarget={} host={} signedHeaders={}",
                        keyId,
                        requestTarget,
                        host,
                        signedHeaders);
            return false;
        }
        var computedDigest = computeDigest(body);
        if (!declaredDigest.equals(computedDigest)) {
            var bodyBytes = body == null ? 0 : body.length();
            logger.warn("ActivityPub inbox signature digest mismatch keyId={} requestTarget={} host={} bodyBytes={}",
                        keyId,
                        requestTarget,
                        host,
                        bodyBytes);
            return false;
        }
        var missingHeader = findMissingSignedHeader(signedHeaders, headerMap);
        if (missingHeader.isPresent()) {
            var headerName = missingHeader.get();
            logger.warn("ActivityPub inbox signature missing signed header keyId={} requestTarget={} host={} header={}",
                        keyId,
                        requestTarget,
                        host,
                        headerName);
            return false;
        }
        var signingString = signingStringFromHeaderList(signedHeaders, headerMap);
        if (!verify(signingString, signature, publicKey)) {
            logger.warn("ActivityPub inbox signature crypto mismatch keyId={} requestTarget={} host={} signedHeaders={}",
                        keyId,
                        requestTarget,
                        host,
                        signedHeaders);
            return false;
        }
        return true;
    }
}
