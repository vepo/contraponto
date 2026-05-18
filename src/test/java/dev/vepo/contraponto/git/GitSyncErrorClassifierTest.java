package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class GitSyncErrorClassifierTest {

    @Inject
    GitSyncErrorClassifier classifier;

    @Test
    void classifiesAuthenticationErrors() {
        var result = classifier.classify(new TransportException("Authentication failed: 401 Unauthorized"));
        assertThat(result.kind()).isEqualTo(GitErrorKind.AUTHENTICATION);
        assertThat(result.remediation()).contains("contraponto.git.username");
    }

    @Test
    void classifiesConventionErrors() {
        var result = classifier.classify(new IllegalArgumentException("Directory segments must not contain '..'."));
        assertThat(result.kind()).isEqualTo(GitErrorKind.CONVENTION);
    }

    @Test
    void classifiesNetworkErrors() {
        var result = classifier.classify(new RuntimeException(new IOException("Connection timed out")));
        assertThat(result.kind()).isEqualTo(GitErrorKind.NETWORK);
    }
}
