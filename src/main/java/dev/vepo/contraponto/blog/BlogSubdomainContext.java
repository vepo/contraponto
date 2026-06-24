package dev.vepo.contraponto.blog;

import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class BlogSubdomainContext {

    private String subdomainUsername;

    public void activate(String username) {
        this.subdomainUsername = username;
    }

    public boolean onUserSubdomain() {
        return subdomainUsername != null && !subdomainUsername.isBlank();
    }

    public Optional<String> subdomainUsername() {
        return Optional.ofNullable(subdomainUsername);
    }
}
