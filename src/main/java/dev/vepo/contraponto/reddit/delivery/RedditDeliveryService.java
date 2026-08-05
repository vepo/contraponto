package dev.vepo.contraponto.reddit.delivery;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.BlogSubdomainConfig;
import dev.vepo.contraponto.notification.NotificationService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.reddit.RedditApiClient;
import dev.vepo.contraponto.reddit.RedditSettings;
import dev.vepo.contraponto.reddit.RedditSubmissionBuilder;
import dev.vepo.contraponto.reddit.account.RedditTokenCipherService;
import dev.vepo.contraponto.reddit.account.UserRedditAccount;
import dev.vepo.contraponto.reddit.account.UserRedditAccountRepository;
import dev.vepo.contraponto.user.User;

@ApplicationScoped
public class RedditDeliveryService {

    private static final Logger logger = LoggerFactory.getLogger(RedditDeliveryService.class);

    private final RedditSettings settings;
    private final RedditDeliveryRepository deliveryRepository;
    private final RedditPostRecordRepository postRecordRepository;
    private final UserRedditAccountRepository accountRepository;
    private final RedditApiClient apiClient;
    private final RedditTokenCipherService cipherService;
    private final PostRepository postRepository;
    private final BlogSubdomainConfig subdomainConfig;
    private final NotificationService notificationService;

    @Inject
    public RedditDeliveryService(RedditSettings settings,
                                 RedditDeliveryRepository deliveryRepository,
                                 RedditPostRecordRepository postRecordRepository,
                                 UserRedditAccountRepository accountRepository,
                                 RedditApiClient apiClient,
                                 RedditTokenCipherService cipherService,
                                 PostRepository postRepository,
                                 BlogSubdomainConfig subdomainConfig,
                                 NotificationService notificationService) {
        this.settings = settings;
        this.deliveryRepository = deliveryRepository;
        this.postRecordRepository = postRecordRepository;
        this.accountRepository = accountRepository;
        this.apiClient = apiClient;
        this.cipherService = cipherService;
        this.postRepository = postRepository;
        this.subdomainConfig = subdomainConfig;
        this.notificationService = notificationService;
    }

    private void deliverCreate(RedditDelivery delivery, UserRedditAccount account, String accessToken) {
        var request = RedditSubmissionBuilder.build(delivery.getPost(), account.getRedditUsername(), subdomainConfig);
        var result = apiClient.submitLinkPost(accessToken, request.subreddit(), request.title(), request.url());
        postRecordRepository.create(new RedditPostRecord(delivery.getPost(), result.fullname(), result.permalink()));
    }

    private void deliverDelete(RedditDelivery delivery, String accessToken) {
        var record = postRecordRepository.findByPostId(delivery.getPost().getId()).orElse(null);
        if (record == null) {
            return;
        }
        apiClient.deleteSubmission(accessToken, record.getSubmissionFullname());
        postRecordRepository.delete(record);
    }

    private void deliverOne(RedditDelivery delivery) {
        var account = accountRepository.findByUserId(delivery.getUser().getId()).orElse(null);
        if (account == null) {
            // Owner disconnected their Reddit account after this delivery was queued —
            // nothing to deliver to.
            delivery.markFailed("Reddit account no longer linked", null);
            deliveryRepository.update(delivery);
            return;
        }
        try {
            var accessToken = refreshAccessToken(account);
            if (delivery.getKind() == RedditDeliveryKind.CREATE) {
                deliverCreate(delivery, account, accessToken);
            } else {
                deliverDelete(delivery, accessToken);
            }
            delivery.markDelivered();
        } catch (RuntimeException ex) {
            // Catches more than RedditApiException on purpose: a corrupt/undecryptable
            // stored token or any other unexpected failure must still mark this delivery
            // failed-and-retried, not crash the whole scheduled batch (that would wedge
            // this entry — and everything queued behind it — forever).
            delivery.markFailed(ex.getMessage(), nextRetry(delivery.getAttempts() + 1));
            logDeliveryFailure(delivery, ex);
        }
        deliveryRepository.update(delivery);
        if (delivery.isExhausted()) {
            notificationService.notifyRedditSyndicationFailed(delivery.getUser(), delivery.getPost());
        }
    }

    private void enqueue(long postId, User owner, RedditDeliveryKind kind) {
        var post = postRepository.findByIdWithBlog(postId).orElse(null);
        if (post == null || post.getBlog() == null || !post.getBlog().isActive()) {
            return;
        }
        deliveryRepository.create(new RedditDelivery(post, owner, kind));
    }

    public void enqueueCreateForPublishedPost(long postId, User owner) {
        // FQ6: republish never re-touches Reddit — a post record only exists once a
        // prior CREATE has actually been delivered, so this also correctly re-creates
        // after an unpublish (which deletes the record) followed by a fresh publish.
        if (postRecordRepository.findByPostId(postId).isPresent()) {
            return;
        }
        enqueue(postId, owner, RedditDeliveryKind.CREATE);
    }

    public void enqueueDeleteForUnpublishedPost(long postId, User owner) {
        enqueue(postId, owner, RedditDeliveryKind.DELETE);
    }

    private void logDeliveryFailure(RedditDelivery delivery, RuntimeException ex) {
        if (delivery.isExhausted()) {
            logger.warn("Reddit delivery exhausted retries id={} kind={} postId={} attempts={} error={}",
                        delivery.getId(),
                        delivery.getKind(),
                        delivery.getPost().getId(),
                        delivery.getAttempts(),
                        ex.getMessage());
            return;
        }
        logger.warn("Reddit delivery failed id={} kind={} postId={} attempts={} error={}",
                    delivery.getId(),
                    delivery.getKind(),
                    delivery.getPost().getId(),
                    delivery.getAttempts(),
                    ex.getMessage());
    }

    private LocalDateTime nextRetry(int attempts) {
        var delaySeconds = Math.min(3600, (long) Math.pow(2, attempts) * 30L);
        return LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(delaySeconds);
    }

    public void processPendingDeliveries() {
        if (!settings.enabled()) {
            return;
        }
        var now = LocalDateTime.now(ZoneId.systemDefault());
        for (var delivery : deliveryRepository.findPendingReady(now)) {
            deliverOne(delivery);
        }
    }

    private String refreshAccessToken(UserRedditAccount account) {
        var refreshToken = cipherService.decrypt(account.getEncryptedRefreshToken());
        var tokenResponse = apiClient.refreshAccessToken(refreshToken);
        if (tokenResponse.refreshToken() != null && !tokenResponse.refreshToken().isBlank()) {
            account.setEncryptedRefreshToken(cipherService.encrypt(tokenResponse.refreshToken()));
            accountRepository.update(account);
        }
        return tokenResponse.accessToken();
    }
}
