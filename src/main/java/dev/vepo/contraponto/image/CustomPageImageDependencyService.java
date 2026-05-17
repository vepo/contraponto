package dev.vepo.contraponto.image;

import java.util.Set;

import dev.vepo.contraponto.custompage.CustomPage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CustomPageImageDependencyService {

    private final ContentImageMarkerService markerService;
    private final ImageRepository imageRepository;
    private final ImageDependencyRepository dependencyRepository;

    @Inject
    public CustomPageImageDependencyService(ContentImageMarkerService markerService,
                                            ImageRepository imageRepository,
                                            ImageDependencyRepository dependencyRepository) {
        this.markerService = markerService;
        this.imageRepository = imageRepository;
        this.dependencyRepository = dependencyRepository;
    }

    public String normalizeAndStoreContent(CustomPage page, String content) {
        String stored = markerService.toStoredContent(content);
        page.setContent(stored);
        return stored;
    }

    @Transactional
    public void syncDependencies(CustomPage page) {
        if (page.getId() == null || page.getBlog() == null) {
            return;
        }
        dependencyRepository.deleteCustomPageDependencies(page.getId());
        Set<String> uuids = markerService.extractImageUuids(page.getContent());
        for (String uuid : uuids) {
            imageRepository.findByUuid(uuid).ifPresent(image -> {
                dependencyRepository.persistCustomPageDependency(new CustomPageImageDependency(page,
                                                                                               image,
                                                                                               ImageRole.INLINE));
            });
        }
    }
}
