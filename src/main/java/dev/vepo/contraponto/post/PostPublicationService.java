package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.contraponto.image.PostImageDependencyService;
import dev.vepo.contraponto.notification.PostPublishedEvent;
import dev.vepo.contraponto.content.render.PostContentRenderer;
import dev.vepo.contraponto.tag.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostPublicationService {

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Set<Long> tagIds(List<Tag> tags) {
        return tags.stream()
                   .map(Tag::getId)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toSet());
    }

    private final PostPublicationRepository publicationRepository;
    private final PostImageDependencyService postImageDependencyService;
    private final PostSlugAliasRepository postSlugAliasRepository;
    private final PostRepository postRepository;
    private final PostContentRenderer postContentRenderer;
    private final Event<PostPublishedEvent> postPublishedEvents;

    @Inject
    public PostPublicationService(PostPublicationRepository publicationRepository,
                                  PostImageDependencyService postImageDependencyService,
                                  PostSlugAliasRepository postSlugAliasRepository,
                                  PostRepository postRepository,
                                  PostContentRenderer postContentRenderer,
                                  Event<PostPublishedEvent> postPublishedEvents) {
        this.publicationRepository = publicationRepository;
        this.postImageDependencyService = postImageDependencyService;
        this.postSlugAliasRepository = postSlugAliasRepository;
        this.postRepository = postRepository;
        this.postContentRenderer = postContentRenderer;
        this.postPublishedEvents = postPublishedEvents;
    }

    /**
     * Aligns {@link Post#getPublishedAt()} and the live {@link PostPublication}
     * timestamp after Git import when only the publish date changed (publish skips
     * an identical content snapshot).
     */
    public void alignPublicationTimestampFromGit(Post post) {
        if (!post.isPublished()) {
            return;
        }
        LocalDateTime publishedAt = post.getPublishedAt();
        if (publishedAt == null) {
            return;
        }
        PostPublication live = post.getLivePublication();
        if (live != null && !Objects.equals(live.getPublishedAt(), publishedAt)) {
            live.setPublishedAt(publishedAt);
        }
    }

    public boolean hasUnpublishedChanges(Post post) {
        if (!post.isPublished() || post.getLivePublication() == null) {
            return false;
        }
        return !isIdenticalSnapshot(post, post.getLivePublication());
    }

    private boolean isIdenticalSnapshot(Post post, PostPublication live) {
        if (!Objects.equals(nullToEmpty(post.getSlug()), nullToEmpty(live.getSlug()))) {
            return false;
        }
        if (!Objects.equals(nullToEmpty(post.getTitle()), nullToEmpty(live.getTitle()))) {
            return false;
        }
        if (!Objects.equals(PostPublicationDescriptions.truncateForPublication(post.getDescription()),
                            nullToEmpty(live.getDescription()))) {
            return false;
        }
        if (!Objects.equals(nullToEmpty(post.getContent()), nullToEmpty(live.getContent()))) {
            return false;
        }
        if (post.getFormat() != live.getFormat()) {
            return false;
        }
        Long postCoverId = post.getCover() != null ? post.getCover().getId() : null;
        Long liveCoverId = live.getCover() != null ? live.getCover().getId() : null;
        if (!Objects.equals(postCoverId, liveCoverId)) {
            return false;
        }
        return tagIds(post.getTags()).equals(tagIds(live.getTags()));
    }

    @Transactional
    public PostPublication publish(Post post) {
        PostPublication candidate = snapshotFrom(post);
        PostPublication live = post.getLivePublication();
        if (live != null && isIdenticalSnapshot(post, live)) {
            return live;
        }
        if (live != null && !Objects.equals(nullToEmpty(post.getSlug()), nullToEmpty(live.getSlug()))) {
            postSlugAliasRepository.saveIfAbsent(post, live.getSlug());
            postRepository.updateSlug(post.getId(), post.getSlug());
        }

        int nextVersion = publicationRepository.findMaxVersion(post.getId()).orElse(0) + 1;
        candidate.setVersion(nextVersion);
        candidate.setPublishedAt(resolvePublicationTimestamp(post, live));
        candidate.setRenderedHtml(postContentRenderer.renderBody(candidate.getContent(), candidate.getFormat()));
        publicationRepository.save(candidate);
        publicationRepository.flush();
        postImageDependencyService.snapshotPublicationDependencies(candidate, post);
        post.setPublished(true);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(candidate.getPublishedAt());
        }
        publicationRepository.findLatestByPostId(post.getId()).ifPresent(post::setLivePublication);
        PostPublication published = publicationRepository.findLatestByPostId(post.getId()).orElse(candidate);
        long authorUserId = post.getBlog().getOwner().getId();
        postPublishedEvents.fire(new PostPublishedEvent(post.getId(),
                                                        published.getId(),
                                                        post.getBlog().getId(),
                                                        authorUserId));
        return published;
    }

    private LocalDateTime resolvePublicationTimestamp(Post post, PostPublication live) {
        boolean firstPublication = live == null;
        if (firstPublication && post.getPublishedAt() != null) {
            return post.getPublishedAt();
        }
        return LocalDateTime.now(ZoneId.systemDefault());
    }

    public PostPublication snapshotFrom(Post post) {
        PostPublication publication = new PostPublication();
        publication.setPost(post);
        publication.setSlug(post.getSlug());
        publication.setTitle(post.getTitle());
        publication.setDescription(PostPublicationDescriptions.truncateForPublication(post.getDescription()));
        publication.setContent(post.getContent());
        publication.setFormat(post.getFormat());
        publication.setCover(post.getCover());
        publication.setTags(new ArrayList<>(post.getTags()));
        return publication;
    }
}
