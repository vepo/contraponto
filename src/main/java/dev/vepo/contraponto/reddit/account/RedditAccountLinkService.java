package dev.vepo.contraponto.reddit.account;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.notification.NotificationService;
import dev.vepo.contraponto.reddit.RedditApiClient;
import dev.vepo.contraponto.reddit.RedditApiException;
import dev.vepo.contraponto.user.User;

@ApplicationScoped
public class RedditAccountLinkService {

    private final UserRedditAccountRepository accountRepository;
    private final RedditApiClient apiClient;
    private final RedditTokenCipherService cipherService;
    private final BlogRepository blogRepository;
    private final NotificationService notificationService;

    @Inject
    public RedditAccountLinkService(UserRedditAccountRepository accountRepository,
                                    RedditApiClient apiClient,
                                    RedditTokenCipherService cipherService,
                                    BlogRepository blogRepository,
                                    NotificationService notificationService) {
        this.accountRepository = accountRepository;
        this.apiClient = apiClient;
        this.cipherService = cipherService;
        this.blogRepository = blogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Exchanges the OAuth code, resolves the Reddit username, and links (or
     * re-links) the account.
     */
    @Transactional
    public UserRedditAccount link(User user, String authorizationCode) {
        var tokenResponse = apiClient.exchangeAuthorizationCode(authorizationCode);
        if (tokenResponse.refreshToken() == null || tokenResponse.refreshToken().isBlank()) {
            throw new RedditApiException("Reddit did not return a refresh token — request duration=permanent");
        }
        var username = apiClient.fetchUsername(tokenResponse.accessToken());
        var encryptedRefreshToken = cipherService.encrypt(tokenResponse.refreshToken());

        var existing = accountRepository.findByUserId(user.getId()).orElse(null);
        if (existing != null) {
            existing.setRedditUsername(username);
            existing.setEncryptedRefreshToken(encryptedRefreshToken);
            return accountRepository.update(existing);
        }
        return accountRepository.create(new UserRedditAccount(user, username, encryptedRefreshToken));
    }

    /**
     * Revokes the Reddit token, deletes the link, and disables Reddit syndication
     * on every blog the author owns — so no delivery is silently attempted for an
     * account that no longer authorizes it (FQ8).
     */
    @Transactional
    public void unlink(User user) {
        var account = accountRepository.findByUserId(user.getId()).orElse(null);
        if (account == null) {
            return;
        }
        try {
            apiClient.revokeToken(cipherService.decrypt(account.getEncryptedRefreshToken()));
        } catch (RuntimeException ignored) {
            // Best-effort revoke: proceed with local unlink even if Reddit is unreachable,
            // the token was already invalid, or the stored ciphertext can't be decrypted —
            // the stale token must not stay usable locally regardless of why revoke failed.
        }
        accountRepository.delete(account);

        for (var blog : blogRepository.findByOwnerIdForManagement(user.getId())) {
            if (blog.isRedditSyndicationEnabled()) {
                blog.setRedditSyndicationEnabled(false);
                blogRepository.save(blog);
            }
        }
        notificationService.notifyRedditAccountDisconnected(user);
    }
}
