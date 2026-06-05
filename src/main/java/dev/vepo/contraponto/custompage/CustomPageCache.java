package dev.vepo.contraponto.custompage;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * In-memory cache of published custom pages for public reads (invalidated on
 * {@link CustomPageChangedEvent}).
 */
@ApplicationScoped
public class CustomPageCache {

    private static String cacheKey(CustomPage page) {
        var blog = page.getBlog();
        var slug = CustomPagePaths.pathSlug(page.getSlug());
        if (blog == null) {
            return keyGlobal(slug);
        }
        var owner = blog.getOwner();
        if (blog.isMain()) {
            return keyUser(owner.getUsername(), slug);
        }
        return keyBlog(owner.getUsername(), blog.getSlug(), slug);
    }

    private static String keyBlog(String username, String blogSlug, String slug) {
        return "blog:%s:%s:%s".formatted(username, blogSlug, CustomPagePaths.storedSlug(slug));
    }

    private static String keyGlobal(String slug) {
        return "global:%s".formatted(CustomPagePaths.storedSlug(slug));
    }

    private static String keyUser(String username, String slug) {
        return "user:%s:%s".formatted(username, CustomPagePaths.storedSlug(slug));
    }

    private final CustomPageRepository customPageRepository;

    private final ConcurrentMap<String, CustomPage> entries = new ConcurrentHashMap<>();

    @Inject
    public CustomPageCache(CustomPageRepository customPageRepository) {
        this.customPageRepository = customPageRepository;
    }

    public Optional<CustomPage> findByUsernameAndSlug(String username, String slug) {
        var key = keyUser(username, slug);
        return getCached(key).or(() -> loadAndRemember(() -> customPageRepository.findByUsernameAndSlug(username, slug),
                                                       key));
    }

    public Optional<CustomPage> findByUsernameBlogSlugAndSlug(String username, String blogSlug, String slug) {
        var key = keyBlog(username, blogSlug, slug);
        return getCached(key)
                             .or(() -> loadAndRemember(() -> customPageRepository.findByUsernameBlogSlugAndSlug(username,
                                                                                                                blogSlug,
                                                                                                                slug),
                                                       key));
    }

    public Optional<CustomPage> findGlobalBySlug(String slug) {
        return getCached(keyGlobal(slug)).or(() -> loadAndRemember(() -> customPageRepository.findGlobalBySlug(slug),
                                                                   keyGlobal(slug)));
    }

    private Optional<CustomPage> getCached(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    public void invalidateById(long pageId) {
        entries.entrySet().removeIf(entry -> Objects.equals(pageId, entry.getValue().getId()));
    }

    private Optional<CustomPage> loadAndRemember(java.util.function.Supplier<Optional<CustomPage>> loader, String key) {
        return loader.get().map(page -> remember(page, key));
    }

    public void refresh(long pageId) {
        customPageRepository.findByIdForManagement(pageId).ifPresentOrElse(this::replace, () -> invalidateById(pageId));
    }

    private CustomPage remember(CustomPage page, String primaryKey) {
        if (!page.isPublished()) {
            entries.remove(primaryKey);
            return page;
        }
        customPageRepository.detach(page);
        entries.put(primaryKey, page);
        return page;
    }

    private void replace(CustomPage page) {
        invalidateById(page.getId());
        if (!page.isPublished()) {
            return;
        }
        customPageRepository.detach(page);
        entries.put(cacheKey(page), page);
    }
}
