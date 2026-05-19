package dev.vepo.contraponto.git;

import java.util.Optional;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Eager snapshot of {@link ContrapontoGitConfig} so Git sync beans can be used
 * from background threads ({@code ConfigMapping} synthesis requires a normal
 * injection point).
 */
@Startup
@ApplicationScoped
public class ContrapontoGitSettings {

    private final boolean pollEnabled;
    private final String pollInterval;
    private final Optional<String> username;
    private final Optional<String> password;
    private final Optional<String> workspaceRoot;

    @Inject
    public ContrapontoGitSettings(ContrapontoGitConfig config) {
        this.pollEnabled = config.pollEnabled();
        this.pollInterval = config.pollInterval();
        this.username = config.username();
        this.password = config.password();
        this.workspaceRoot = config.workspaceRoot();
    }

    public Optional<String> password() {
        return password;
    }

    public boolean pollEnabled() {
        return pollEnabled;
    }

    public String pollInterval() {
        return pollInterval;
    }

    public Optional<String> username() {
        return username;
    }

    public Optional<String> workspaceRoot() {
        return workspaceRoot;
    }
}
