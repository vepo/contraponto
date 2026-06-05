package dev.vepo.contraponto.auth;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AccountEmailOutboxScheduler {

    private final AccountEmailOutboxService outboxService;

    @Inject
    public AccountEmailOutboxScheduler(AccountEmailOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Scheduled(every = "${app.account-email.outbox.retry-interval}", concurrentExecution = ConcurrentExecution.SKIP)
    void retryPendingAccountEmails() {
        outboxService.processDue();
    }
}
