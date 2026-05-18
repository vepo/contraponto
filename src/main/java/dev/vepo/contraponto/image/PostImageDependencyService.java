package dev.vepo.contraponto.image;

import java.util.Set;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class PostImageDependencyService {

    private final ContentImageMarkerService markerService;
    private final ImageRepository imageRepository;
    private final ImageDependencyRepository dependencyRepository;

    @Inject
    public PostImageDependencyService(ContentImageMarkerService markerService,
                                      ImageRepository imageRepository,
                                      ImageDependencyRepository dependencyRepository) {
        this.markerService = markerService;
        this.imageRepository = imageRepository;
        this.dependencyRepository = dependencyRepository;
    }

    public String normalizeAndStoreContent(Post post, String editorContent) {
        String stored = markerService.toStoredContent(editorContent);
        post.setContent(stored);
        return stored;
    }

    @Transactional
    public void snapshotPublicationDependencies(PostPublication publication, Post post) {
        if (publication.getId() == null) {
            return;
        }
        dependencyRepository.deletePublicationDependencies(publication.getId());
        Set<String> uuids = markerService.extractImageUuids(publication.getContent());
        for (String uuid : uuids) {
            imageRepository.findByUuid(uuid).ifPresent(image ->
                    dependencyRepository.persistPublicationDependency(new PostPublicationImageDependency(publication,
                                                                                                         image,
                                                                                                         ImageRole.INLINE)));
        }
        if (publication.getCover() != null) {
            dependencyRepository.persistPublicationDependency(new PostPublicationImageDependency(publication,
                                                                                                 publication.getCover(),
                                                                                                 ImageRole.COVER));
        }
    }

    @Transactional
    public void syncPostDependencies(Post post) {
        if (post.getId() == null) {
            return;
        }
        dependencyRepository.deletePostDependencies(post.getId());
        Set<String> uuids = markerService.extractImageUuids(post.getContent());
        for (String uuid : uuids) {
            imageRepository.findByUuid(uuid).ifPresent(image ->
                    dependencyRepository.persistPostDependency(new PostImageDependency(post, image, ImageRole.INLINE)));
        }
        if (post.getCover() != null) {
            dependencyRepository.persistPostDependency(new PostImageDependency(post, post.getCover(), ImageRole.COVER));
        }
    }
}
