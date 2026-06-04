package dev.vepo.contraponto.git;

import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GitSyncErrorClassifier {

    public record ClassifiedError(GitErrorKind kind, String message, String remediation) {}

    public static String defaultRemediation(GitErrorKind kind) {
        return switch (kind) {
            case AUTHENTICATION -> "Configure server Git credentials or use a token in the HTTPS URL.";
            case NETWORK -> "Retry when the network and Git host are available.";
            case REPOSITORY -> "Verify remote URL and branch on the blog settings page.";
            case WORKSPACE -> "Ask an administrator to reset the Git workspace directory.";
            case CONVENTION -> "Review _contraponto.yml and the Jekyll layout convention.";
            case POST -> "Open the sync log entry for the affected post and follow How to fix.";
            case NONE -> null;
            case UNKNOWN -> "Open Git sync history for details or contact support.";
        };
    }

    private static boolean isAuthMessage(String text) {
        return text.contains("authentication") || text.contains("not authorized")
                || text.contains("401") || text.contains("403")
                || text.contains("invalid credentials") || text.contains("access denied");
    }

    private static Throwable rootCause(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    public ClassifiedError classify(Throwable throwable) {
        if (throwable == null) {
            return new ClassifiedError(GitErrorKind.UNKNOWN, "Unknown error", defaultRemediation(GitErrorKind.UNKNOWN));
        }
        Throwable root = rootCause(throwable);
        String text = (root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName()).toLowerCase();

        if (root instanceof TransportException || isAuthMessage(text)) {
            return new ClassifiedError(GitErrorKind.AUTHENTICATION,
                                       "Git authentication failed.",
                                       "Configure contraponto.git.username and contraponto.git.password on the server, "
                                               + "or embed a personal access token in the HTTPS remote URL.");
        }
        if (root instanceof RepositoryNotFoundException || text.contains("not found") && text.contains("repository")) {
            return new ClassifiedError(GitErrorKind.REPOSITORY,
                                       "Git repository was not found.",
                                       "Check the remote URL and branch name on the blog settings page.");
        }
        if (text.contains("ref") && (text.contains("not found") || text.contains("unknown"))) {
            return new ClassifiedError(GitErrorKind.REPOSITORY,
                                       "Git branch or reference was not found.",
                                       "Verify the branch name matches your remote default branch.");
        }
        if (text.contains("timeout") || text.contains("timed out") || text.contains("connection")
                || text.contains("unreachable") || text.contains("network")) {
            return new ClassifiedError(GitErrorKind.NETWORK,
                                       "Could not reach the Git remote.",
                                       "Check network connectivity and that the Git host is available.");
        }
        if (text.contains("occupied") || text.contains("workspace") || text.contains("not empty")) {
            return new ClassifiedError(GitErrorKind.WORKSPACE,
                                       "Git workspace could not be prepared.",
                                       "Contact your administrator to clear the local Git workspace for this blog.");
        }
        if (text.contains("_contraponto") || text.contains("convention") || text.contains("directory segments")) {
            return new ClassifiedError(GitErrorKind.CONVENTION,
                                       "Repository layout configuration is invalid.",
                                       "Fix _contraponto.yml in the repository root or remove it to use defaults. "
                                               + "See the layout convention documentation.");
        }
        if (text.contains("invalid remote") || text.contains("cannot open") && text.contains("uri")) {
            return new ClassifiedError(GitErrorKind.REPOSITORY,
                                       "Git remote URL is invalid.",
                                       "Use a valid HTTPS Git remote URL on the blog settings page.");
        }
        return new ClassifiedError(GitErrorKind.UNKNOWN,
                                   root.getMessage() != null ? root.getMessage() : "Git sync failed.",
                                   defaultRemediation(GitErrorKind.UNKNOWN));
    }
}
