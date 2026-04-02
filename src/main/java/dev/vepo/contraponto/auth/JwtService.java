package dev.vepo.contraponto.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.user.User;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @ConfigProperty(name = "quarkus.jwt.token.issuer", defaultValue = "contraponto")
    String issuer;

    @ConfigProperty(name = "quarkus.jwt.token.lifetime", defaultValue = "86400")
    Long tokenLifetime; // seconds

    @Inject
    JWTParser jwtParser;

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(tokenLifetime, ChronoUnit.SECONDS);

        JwtClaimsBuilder claims = Jwt.claims()
                                     .subject(user.getId().toString())
                                     .issuer(issuer)
                                     .issuedAt(now)
                                     .expiresAt(expiry)
                                     .claim("email", user.getEmail())
                                     .claim("name", user.getName())
                                     .groups(Set.of("user"));

        return claims.jws().sign();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(7, ChronoUnit.DAYS);

        JwtClaimsBuilder claims = Jwt.claims()
                                     .subject(user.getId().toString())
                                     .issuer(issuer)
                                     .issuedAt(now)
                                     .expiresAt(expiry)
                                     .claim("type", "refresh")
                                     .claim("email", user.getEmail());

        return claims.jws().sign();
    }

    public JsonWebToken validateRefreshToken(String refreshTokenString) {
        try {
            JsonWebToken token = jwtParser.parse(refreshTokenString);

            // Check if it's a refresh token
            String type = token.getClaim("type");
            if (type == null || !"refresh".equals(type)) {
                logger.warn("Token is not a refresh token");
                return null;
            }

            // Check if expired
            long expirationTime = token.getExpirationTime();
            if (expirationTime < System.currentTimeMillis() / 1000) {
                logger.warn("Refresh token has expired");
                return null;
            }

            return token;
        } catch (ParseException e) {
            logger.error("Failed to parse refresh token: {}", e.getMessage());
            return null;
        }
    }
}