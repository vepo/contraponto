package dev.vepo.contraponto.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.shared.Given;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.shared.QuarkusIntegrationTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@QuarkusIntegrationTest
class NotificationRetentionServiceTest {

    @Inject
    NotificationRetentionService retentionService;

    @Inject
    NotificationService notificationService;

    @Inject
    NotificationRepository notificationRepository;

    @Inject
    EntityManager entityManager;

    private User recipient;
    private User actor;
    private Blog blog;

    @Transactional
    void backdateCreatedAt(long notificationId, LocalDateTime createdAt) {
        entityManager.createNativeQuery("""
                                        UPDATE tb_notifications
                                        SET created_at = :createdAt
                                        WHERE id = :id
                                        """)
                     .setParameter("createdAt", createdAt)
                     .setParameter("id", notificationId)
                     .executeUpdate();
    }

    private Notification createFollowNotification() {
        notificationService.notifyNewFollow(recipient, blog, actor);
        return notificationRepository.findUnreadRecent(recipient.getId(), 1).getFirst();
    }

    private LocalDateTime daysAgo(int days) {
        return LocalDateTime.now(ZoneId.systemDefault()).minusDays(days);
    }

    @Transactional
    void markRead(long notificationId, LocalDateTime readAt) {
        entityManager.createNativeQuery("""
                                        UPDATE tb_notifications
                                        SET read = TRUE, read_at = :readAt
                                        WHERE id = :id
                                        """)
                     .setParameter("readAt", readAt)
                     .setParameter("id", notificationId)
                     .executeUpdate();
    }

    @Test
    void purgeExpired_deletesOldReadAndUnreadNotifications() {
        var recentUnread = createFollowNotification();
        var oldUnread = createFollowNotification();
        backdateCreatedAt(oldUnread.getId(), daysAgo(31));

        var recentRead = createFollowNotification();
        markRead(recentRead.getId(), daysAgo(3));

        var oldRead = createFollowNotification();
        markRead(oldRead.getId(), daysAgo(10));

        var result = retentionService.purgeExpired();

        assertThat(result.readDeleted()).isEqualTo(1);
        assertThat(result.unreadDeleted()).isEqualTo(1);
        entityManager.clear();
        assertThat(notificationRepository.findPage(recipient.getId(), dev.vepo.contraponto.shared.pagination.PageQuery.forGrid(20, 1)).total())
                                                                                                                                               .isEqualTo(2);
        assertThat(entityManager.find(Notification.class, recentUnread.getId())).isNotNull();
        assertThat(entityManager.find(Notification.class, recentRead.getId())).isNotNull();
        assertThat(entityManager.find(Notification.class, oldUnread.getId())).isNull();
        assertThat(entityManager.find(Notification.class, oldRead.getId())).isNull();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
        actor = Given.user()
                     .withUsername("retainactor")
                     .withEmail("retainactor@test.com")
                     .withName("Retain Actor")
                     .withPassword("password123")
                     .persist();
        recipient = Given.user()
                         .withUsername("retainuser")
                         .withEmail("retainuser@test.com")
                         .withName("Retain User")
                         .withPassword("password123")
                         .persist();
        blog = actor.getDefaultBlog();
    }
}
