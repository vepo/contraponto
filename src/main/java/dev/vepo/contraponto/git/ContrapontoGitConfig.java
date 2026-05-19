package dev.vepo.contraponto.git;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "contraponto.git")
public interface ContrapontoGitConfig {

    /**
     * Shallow clone/fetch depth ({@code 1} = latest commit only). Use {@code 0} for
     * full history.
     */
    @WithDefault("1")
    int cloneDepth();

    Optional<String> password();

    @WithDefault("true")
    boolean pollEnabled();

    @WithDefault("2m")
    String pollInterval();

    Optional<String> username();

    /**
     * Local clone workspace; omit to use JVM temp under {@code contraponto-git}.
     */
    Optional<String> workspaceRoot();
}
