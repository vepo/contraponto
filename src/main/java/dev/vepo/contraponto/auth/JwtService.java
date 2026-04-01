package dev.vepo.contraponto.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.user.User;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.build.JwtClaimsBuilder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class JwtService {

    @ConfigProperty(name = "quarkus.jwt.token.issuer", defaultValue = "contraponto")
    String issuer;

    @ConfigProperty(name = "quarkus.jwt.token.lifetime", defaultValue = "86400")
    Long tokenLifetime; // seconds

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
}