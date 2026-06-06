package dev.vepo.contraponto.auth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AccountEmailOutboxService {

    private static final Logger logger = LoggerFactory.getLogger(AccountEmailOutboxService.class);

    private static final List<Duration> RETRY_BACKOFF = List.of(
                                                                Duration.ofMinutes(1),
                                                                Duration.ofMinutes(2),
                                                                Duration.ofMinutes(5),
                                                                Duration.ofMinutes(15),
                                                                Duration.ofMinutes(30),
                                                                Duration.ofHours(1));

    private final int batchSize;
    private final AccountEmailOutboxRepository outboxRepository;
    private final AccountEmailService accountEmailService;

    @Inject
    public AccountEmailOutboxService(@ConfigProperty(name = "app.account-email.outbox.batch-size", defaultValue = "20") int batchSize,
                                     AccountEmailOutboxRepository outboxRepository,
                                     AccountEmailService accountEmailService) {
        this.batchSize = batchSize;
        this.outboxRepository = outboxRepository;
        this.accountEmailService = accountEmailService;
    }

    public void processDue() {
        var now = LocalDateTime.now(ZoneId.systemDefault());
        for (AccountEmailOutbox entry : outboxRepository.findDue(now, batchSize)) {
            try {
                accountEmailService.resendOutboxEntry(entry);
                outboxRepository.delete(entry);
            } catch (RuntimeException failure) {
                var attemptCount = entry.getAttemptCount() + 1;
                var nextRetryAt = now.plus(retryDelay(attemptCount));
                logger.warn("Account email outbox retry failed entry={} attempt={}",
                            entry,
                            attemptCount,
                            failure);
                outboxRepository.recordFailure(entry, attemptCount, nextRetryAt, failure.getMessage());
            }
        }
    }

    private Duration retryDelay(int attemptCount) {
        return RETRY_BACKOFF.get(Math.clamp(attemptCount - 1l, 0, RETRY_BACKOFF.size() - 1));
    }
}
