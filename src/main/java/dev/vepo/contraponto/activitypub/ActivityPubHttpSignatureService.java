package dev.vepo.contraponto.activitypub;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ActivityPubHttpSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityPubHttpSignatureService.class);

    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US);

    private static final Pattern SIGNATURE_PARAM = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private final ActivityPubKeyPairService keyPairService;
    private final ActivityPubActorRepository actorRepository;
    private final ActivityPubRemoteActorRepository remoteActorRepository;

    @Inject
    public ActivityPubHttpSignatureService(ActivityPubKeyPairService keyPairService,
                                           ActivityPubActorRepository actorRepository,
                                           ActivityPubRemoteActorRepository remoteActorRepository) {
        this.keyPairService = keyPairService;
        this.actorRepository = actorRepository;
        this.remoteActorRepository = remoteActorRepository;
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
        var path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        var query = uri.getRawQuery();
        if (query != null && !query.isBlank()) {
            path = "%s?%s".formatted(path, query);
        }
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

    private String buildSigningStringFromHeaderList(String signedHeaders, Map<String, String> headerMap) {
        var sb = new StringBuilder();
        var first = true;
        for (var name : signedHeaders.split("\\s+")) {
            var key = name.trim().toLowerCase(Locale.ROOT);
            var value = headerMap.get(key);
            if (value == null) {
                continue;
            }
            if (!first) {
                sb.append('\n');
            }
            sb.append(key).append(": ").append(value);
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

    private String headerValue(Map<String, String> headers, String name) {
        for (var entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, String> parseSignatureHeader(String header) {
        var params = new LinkedHashMap<String, String>();
        Matcher matcher = SIGNATURE_PARAM.matcher(header);
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }
        return params;
    }

    private PublicKey resolvePublicKey(String keyId) {
        var local = actorRepository.findByPublicKeyId(keyId);
        if (local.isPresent()) {
            return keyPairService.parsePublicKeyPem(local.get().getPublicKeyPem());
        }
        var remote = remoteActorRepository.findByPublicKeyId(keyId);
        if (remote.isPresent() && remote.get().getPublicKeyPem() != null) {
            return keyPairService.parsePublicKeyPem(remote.get().getPublicKeyPem());
        }
        return null;
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

    public Map<String, String> signRequest(PrivateKey privateKey,
                                           String keyId,
                                           String method,
                                           URI targetUri,
                                           String body) {
        var date = HTTP_DATE.format(ZonedDateTime.now(ZoneOffset.UTC));
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
            return false;
        }
        var params = parseSignatureHeader(signatureHeader);
        var keyId = params.get("keyId");
        var algorithm = params.get("algorithm");
        var signedHeaders = params.get("headers");
        var signature = params.get("signature");
        if (keyId == null || algorithm == null || signedHeaders == null || signature == null) {
            return false;
        }
        if (!"rsa-sha256".equalsIgnoreCase(algorithm)) {
            return false;
        }
        var publicKey = resolvePublicKey(keyId);
        if (publicKey == null) {
            logger.warn("No public key found for keyId={}", keyId);
            return false;
        }
        return verifySignedHeaders(method, requestUri, body, headers, signedHeaders, signature, publicKey);
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
        return verifySignedHeaders(method, requestUri, body, headers, signedHeaders, signature, publicKey);
    }

    private boolean verifySignedHeaders(String method,
                                        URI requestUri,
                                        String body,
                                        Map<String, String> headers,
                                        String signedHeaders,
                                        String signature,
                                        PublicKey publicKey) {
        var headerMap = buildHeaderMapForVerification(method, requestUri, body, headers);
        var declaredDigest = headerMap.get("digest");
        if (declaredDigest == null || !declaredDigest.equals(computeDigest(body))) {
            return false;
        }
        var signingString = buildSigningStringFromHeaderList(signedHeaders, headerMap);
        return verify(signingString, signature, publicKey);
    }
}
