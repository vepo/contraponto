package dev.vepo.contraponto.blog;

import java.util.Optional;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class BlogSubdomainContext {

    private String subdomainUsername;
    private String signatureRequestPath;

    public void activate(String username) {
        this.subdomainUsername = username;
    }

    public boolean onUserSubdomain() {
        return subdomainUsername != null && !subdomainUsername.isBlank();
    }

    public void setSignatureRequestPath(String path) {
        this.signatureRequestPath = path;
    }

    public Optional<String> signatureRequestPath() {
        return Optional.ofNullable(signatureRequestPath);
    }

    public Optional<String> subdomainUsername() {
        return Optional.ofNullable(subdomainUsername);
    }
}
