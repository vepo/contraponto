package dev.vepo.contraponto.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.eclipse.jgit.api.errors.TransportException;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;

@QuarkusIntegrationTest
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
    void classifiesInvalidRemoteUri() {
        var result = classifier.classify(new RuntimeException("cannot open uri: not-a-url"));
        assertThat(result.kind()).isEqualTo(GitErrorKind.REPOSITORY);
    }

    @Test
    void classifiesMissingBranchReference() {
        var result = classifier.classify(new RuntimeException("ref refs/heads/missing not found"));
        assertThat(result.kind()).isEqualTo(GitErrorKind.REPOSITORY);
        assertThat(result.message()).contains("branch");
    }

    @Test
    void classifiesNetworkErrors() {
        var result = classifier.classify(new RuntimeException(new IOException("Connection timed out")));
        assertThat(result.kind()).isEqualTo(GitErrorKind.NETWORK);
    }

    @Test
    void classifiesNullAsUnknown() {
        var result = classifier.classify(null);
        assertThat(result.kind()).isEqualTo(GitErrorKind.UNKNOWN);
        assertThat(result.remediation()).isEqualTo(GitSyncErrorClassifier.defaultRemediation(GitErrorKind.UNKNOWN));
    }

    @Test
    void classifiesRepositoryNotFound() {
        var result = classifier.classify(new org.eclipse.jgit.errors.RepositoryNotFoundException("repo not found"));
        assertThat(result.kind()).isEqualTo(GitErrorKind.REPOSITORY);
        assertThat(result.message()).contains("not found");
    }

    @Test
    void classifiesWorkspaceOccupied() {
        var result = classifier.classify(new RuntimeException("workspace is not empty"));
        assertThat(result.kind()).isEqualTo(GitErrorKind.WORKSPACE);
    }

    @Test
    void defaultRemediationCoversAllKinds() {
        assertThat(GitSyncErrorClassifier.defaultRemediation(GitErrorKind.NONE)).isNull();
        assertThat(GitSyncErrorClassifier.defaultRemediation(GitErrorKind.AUTHENTICATION)).isNotBlank();
        assertThat(GitSyncErrorClassifier.defaultRemediation(GitErrorKind.NETWORK)).isNotBlank();
        assertThat(GitSyncErrorClassifier.defaultRemediation(GitErrorKind.POST)).isNotBlank();
        assertThat(GitSyncErrorClassifier.defaultRemediation(GitErrorKind.UNKNOWN)).isNotBlank();
    }
}
