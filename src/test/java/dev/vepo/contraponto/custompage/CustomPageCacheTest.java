package dev.vepo.contraponto.custompage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.vepo.contraponto.shared.Given;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@QuarkusTest
class CustomPageCacheTest {

    @Inject
    CustomPageCache customPageCache;

    @Inject
    CustomPageRepository customPageRepository;

    @Inject
    EntityManager entityManager;

    @Test
    void findGlobalBySlugUsesGlobalKey() {
        persistGlobalPage("/global-about", "About");

        assertThat(customPageCache.findGlobalBySlug("global-about")).map(CustomPage::getTitle).contains("About");
    }

    @Test
    void invalidateByIdRemovesMatchingEntry() {
        CustomPage first = persistGlobalPage("/cache-one", "Cache One");
        CustomPage second = persistGlobalPage("/cache-two", "Cache Two");

        assertThat(customPageCache.findGlobalBySlug("cache-one")).map(CustomPage::getTitle).contains("Cache One");
        assertThat(customPageCache.findGlobalBySlug("cache-two")).map(CustomPage::getTitle).contains("Cache Two");

        Given.transaction(() -> {
            var loaded = entityManager.find(CustomPage.class, first.getId());
            loaded.setTitle("Cache One Updated");
        });

        customPageCache.invalidateById(first.getId());

        assertThat(customPageCache.findGlobalBySlug("cache-one")).map(CustomPage::getTitle).contains("Cache One Updated");
        assertThat(customPageCache.findGlobalBySlug("cache-two")).map(CustomPage::getTitle).contains("Cache Two");
    }

    private CustomPage persistGlobalPage(String slug, String title) {
        return Given.transaction(() -> {
            var page = new CustomPage(slug, title, null, "Content", PagePlacement.FOOTER, null, true);
            entityManager.persist(page);
            return page;
        });
    }

    @Test
    void refreshEvictsUnpublishedPage() {
        CustomPage page = persistGlobalPage("/was-published", "Was Published");
        assertThat(customPageCache.findGlobalBySlug("was-published")).isPresent();

        Given.transaction(() -> entityManager.find(CustomPage.class, page.getId()).setPublished(false));

        customPageCache.refresh(page.getId());

        assertThat(customPageCache.findGlobalBySlug("was-published")).isEmpty();
    }

    @Test
    void refreshRemovesDeletedPage() {
        CustomPage page = persistGlobalPage("/refresh-me", "Refresh Me");
        assertThat(customPageCache.findGlobalBySlug("refresh-me")).isPresent();

        customPageRepository.delete(page.getId());
        customPageCache.refresh(page.getId());

        assertThat(customPageCache.findGlobalBySlug("refresh-me")).isEmpty();
    }

    @BeforeEach
    void setUp() {
        Given.cleanup();
    }
}
