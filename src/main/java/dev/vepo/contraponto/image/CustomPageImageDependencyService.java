package dev.vepo.contraponto.image;

import java.util.Set;

import dev.vepo.contraponto.custompage.CustomPage;
import dev.vepo.contraponto.shared.security.HtmlSanitizer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CustomPageImageDependencyService {

    private final ContentImageMarkerService markerService;
    private final ImageRepository imageRepository;
    private final ImageDependencyRepository dependencyRepository;
    private final HtmlSanitizer htmlSanitizer;

    @Inject
    public CustomPageImageDependencyService(ContentImageMarkerService markerService,
                                            ImageRepository imageRepository,
                                            ImageDependencyRepository dependencyRepository,
                                            HtmlSanitizer htmlSanitizer) {
        this.markerService = markerService;
        this.imageRepository = imageRepository;
        this.dependencyRepository = dependencyRepository;
        this.htmlSanitizer = htmlSanitizer;
    }

    public String normalizeAndStoreContent(CustomPage page, String content) {
        String stored = markerService.toStoredContent(content);
        stored = htmlSanitizer.sanitizePostHtml(stored);
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
            imageRepository.findByUuid(uuid).ifPresent(image -> dependencyRepository.persistCustomPageDependency(new CustomPageImageDependency(page,
                                                                                                                                               image,
                                                                                                                                               ImageRole.INLINE)));
        }
    }
}
