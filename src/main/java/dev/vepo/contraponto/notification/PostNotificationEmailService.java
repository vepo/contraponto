package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.shared.infra.SiteBranding;
import dev.vepo.contraponto.user.User;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PostNotificationEmailService {

    private final Mailer mailer;
    private final EmailNotificationLogRepository logRepository;
    private final Template postPublishedEmail;
    private final String mailFrom;
    private final String baseUrl;
    private final SiteBranding siteBranding;

    @Inject
    public PostNotificationEmailService(Mailer mailer,
                                        EmailNotificationLogRepository logRepository,
                                        @Location("notification/post-published-email") Template postPublishedEmail,
                                        @ConfigProperty(name = "quarkus.mailer.from", defaultValue = "noreply@contraponto.blog") String mailFrom,
                                        @ConfigProperty(name = "image.base.url", defaultValue = "http://localhost:8080") String baseUrl,
                                        SiteBranding siteBranding) {
        this.mailer = mailer;
        this.logRepository = logRepository;
        this.postPublishedEmail = postPublishedEmail;
        this.mailFrom = mailFrom;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.siteBranding = siteBranding;
    }

    @Transactional
    public void sendIfNotSent(User subscriber, Post post, PostPublication publication, Blog blog) {
        if (logRepository.exists(publication.getId(), subscriber.getId())) {
            return;
        }

        String postUrl = baseUrl + PostEndpoint.extractUrl(post);
        String title = publication.getTitle() != null ? publication.getTitle() : publication.getSlug();
        String excerpt = publication.getDescription();
        if (excerpt == null || excerpt.isBlank()) {
            excerpt = "";
        }

        String html = postPublishedEmail.data("blogName", blog.getName())
                                        .data("postTitle", title)
                                        .data("postUrl", postUrl)
                                        .data("baseUrl", baseUrl)
                                        .data("excerpt", excerpt)
                                        .data("blogId", blog.getId())
                                        .data("siteName", siteBranding.displayName())
                                        .data("siteSeoName", siteBranding.seoName())
                                        .render();

        String subject = blog.getName() + ": " + title;

        mailer.send(Mail.withHtml(subscriber.getEmail(), subject, html).setFrom(mailFrom));

        logRepository.persist(new EmailNotificationLog(publication, subscriber));
    }
}
