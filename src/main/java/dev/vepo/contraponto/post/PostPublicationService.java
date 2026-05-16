package dev.vepo.contraponto.post;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import dev.vepo.contraponto.tag.Tag;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    @Inject
    public PostPublicationService(PostPublicationRepository publicationRepository,
                                  EntityManager entityManager) {
        this.publicationRepository = publicationRepository;
        this.entityManager = entityManager;
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
        if (!Objects.equals(nullToEmpty(post.getDescription()), nullToEmpty(live.getDescription()))) {
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

        int nextVersion = publicationRepository.findMaxVersion(post.getId()).orElse(0) + 1;
        candidate.setVersion(nextVersion);
        candidate.setPublishedAt(LocalDateTime.now());
        publicationRepository.save(candidate);
        entityManager.flush();
        post.setPublished(true);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(candidate.getPublishedAt());
        }
        publicationRepository.findLatestByPostId(post.getId()).ifPresent(post::setLivePublication);
        return publicationRepository.findLatestByPostId(post.getId()).orElse(candidate);
    }

    public PostPublication snapshotFrom(Post post) {
        PostPublication publication = new PostPublication();
        publication.setPost(post);
        publication.setSlug(post.getSlug());
        publication.setTitle(post.getTitle());
        publication.setDescription(post.getDescription());
        publication.setContent(post.getContent());
        publication.setFormat(post.getFormat());
        publication.setCover(post.getCover());
        publication.setTags(new ArrayList<>(post.getTags()));
        return publication;
    }
}
