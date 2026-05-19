package dev.vepo.contraponto.custompage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.image.CustomPageImageDependencyService;
import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/pages")
public class CustomPageSaveEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomPageSaveEndpoint.class);

    private final CustomPageRepository customPageRepository;
    private final CustomPageAccess customPageAccess;
    private final BlogRepository blogRepository;
    private final CustomPageImageDependencyService customPageImageDependencyService;
    private final NavigationHubService navigationHubService;
    private final LoggedUser loggedUser;
    private final Event<CustomPageChangedEvent> customPageChangedEvents;

    @Inject
    public CustomPageSaveEndpoint(CustomPageRepository customPageRepository,
                                  CustomPageAccess customPageAccess,
                                  BlogRepository blogRepository,
                                  CustomPageImageDependencyService customPageImageDependencyService,
                                  NavigationHubService navigationHubService,
                                  LoggedUser loggedUser,
                                  Event<CustomPageChangedEvent> customPageChangedEvents) {
        this.customPageRepository = customPageRepository;
        this.customPageAccess = customPageAccess;
        this.blogRepository = blogRepository;
        this.customPageImageDependencyService = customPageImageDependencyService;
        this.navigationHubService = navigationHubService;
        this.loggedUser = loggedUser;
        this.customPageChangedEvents = customPageChangedEvents;
    }

    private boolean applyScope(CustomPage page, CustomPageForm form, Long blogIdForSlug) {
        if (form.isApplicationScope()) {
            if (!customPageAccess.canManageApplicationPages(loggedUser)) {
                return false;
            }
            page.setBlog(null);
            return true;
        }

        if (blogIdForSlug == null) {
            return false;
        }

        var blog = blogRepository.findById(blogIdForSlug).orElse(null);
        if (blog == null || !customPageAccess.canEditBlog(blog, loggedUser)) {
            return false;
        }
        page.setBlog(blog);
        return true;
    }

    @DELETE
    @Path("{id}")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response delete(@PathParam("id") long id) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var page = customPageRepository.findByIdForManagement(id).orElse(null);
        if (page == null) {
            return notFound();
        }
        if (!customPageAccess.canEdit(page, loggedUser)) {
            return forbidden();
        }

        long pageId = page.getId();
        customPageRepository.delete(id);
        customPageChangedEvents.fire(new CustomPageChangedEvent(pageId));

        return Toast.ok()
                    .i18nKey(I18nKeys.TOAST_PAGE_DELETED, I18nDefaults.PAGE_DELETED)
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .url("/manage/pages")
                    .page(navigationHubService.shell(NavigationHub.MANAGE, "pages", 1))
                    .build();
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .i18nKey(I18nKeys.TOAST_PAGE_FORBIDDEN, I18nDefaults.PAGE_FORBIDDEN)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Response notFound() {
        return Toast.response(Response.Status.NOT_FOUND)
                    .i18nKey(I18nKeys.TOAST_PAGE_NOT_FOUND, I18nDefaults.PAGE_NOT_FOUND)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private Long resolveBlogId(CustomPageForm form, dev.vepo.contraponto.blog.Blog currentBlog) {
        if (form.isApplicationScope()) {
            return null;
        }
        if (form.getBlogId() != null) {
            return form.getBlogId();
        }
        return currentBlog != null ? currentBlog.getId() : null;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(@BeanParam CustomPageForm form) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var validationError = validate(form);
        if (validationError != null) {
            return Toast.response(Response.Status.BAD_REQUEST)
                        .message(validationError)
                        .type(Toast.Type.ERROR)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        }

        var resolved = resolvePageForSave(form);
        if (resolved.errorResponse() != null) {
            return resolved.errorResponse();
        }
        CustomPage page = resolved.page();
        Long blogIdForSlug = resolved.blogIdForSlug();

        if (customPageRepository.existsSlug(form.getSlug(), blogIdForSlug, page.getId())) {
            return Toast.response(Response.Status.BAD_REQUEST)
                        .i18nKey(I18nKeys.TOAST_PAGE_SLUG_EXISTS, I18nDefaults.PAGE_SLUG_EXISTS)
                        .type(Toast.Type.ERROR)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .build();
        }

        page.setTitle(form.getTitle().trim());
        page.setSlug(CustomPagePaths.storedSlug(form.getSlug().trim()));
        page.setSection(form.getSection().trim());
        customPageImageDependencyService.normalizeAndStoreContent(page, form.getContent());
        page.setPlacement(form.getPlacement());
        page.setPublished(form.isPublished());

        customPageRepository.save(page);
        customPageImageDependencyService.syncDependencies(page);
        customPageChangedEvents.fire(new CustomPageChangedEvent(page.getId()));
        logger.info("Saved custom page id={} slug={}", page.getId(), page.getSlug());

        return Toast.ok()
                    .i18nKey(I18nKeys.TOAST_PAGE_SAVED, I18nDefaults.PAGE_SAVED)
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .url("/manage/pages")
                    .page(navigationHubService.shell(NavigationHub.MANAGE, "pages", 1))
                    .build();
    }

    private PageSaveResolution resolvePageForSave(CustomPageForm form) {
        if (form.getId() != null) {
            var page = customPageRepository.findByIdForManagement(form.getId()).orElse(null);
            if (page == null) {
                return PageSaveResolution.error(notFound());
            }
            if (!customPageAccess.canEdit(page, loggedUser)) {
                return PageSaveResolution.error(forbidden());
            }
            Long blogIdForSlug = resolveBlogId(form, page.getBlog());
            if (!applyScope(page, form, blogIdForSlug)) {
                return PageSaveResolution.error(forbidden());
            }
            return PageSaveResolution.ok(page, blogIdForSlug);
        }

        Long blogIdForSlug = resolveBlogId(form, null);
        if (blogIdForSlug == null && !form.isApplicationScope()) {
            return PageSaveResolution.error(Toast.response(Response.Status.BAD_REQUEST)
                                                 .i18nKey(I18nKeys.TOAST_PAGE_BLOG_REQUIRED, I18nDefaults.PAGE_BLOG_REQUIRED)
                                                 .type(Toast.Type.ERROR)
                                                 .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                                                 .build());
        }
        if (!form.isApplicationScope() && blogIdForSlug != null) {
            var blog = blogRepository.findById(blogIdForSlug).orElse(null);
            if (blog == null || !customPageAccess.canEditBlog(blog, loggedUser)) {
                return PageSaveResolution.error(forbidden());
            }
            return PageSaveResolution.ok(customPageRepository.newPage(blog), blogIdForSlug);
        }
        if (form.isApplicationScope()) {
            if (!customPageAccess.canManageApplicationPages(loggedUser)) {
                return PageSaveResolution.error(forbidden());
            }
            return PageSaveResolution.ok(customPageRepository.newPage(null), blogIdForSlug);
        }
        return PageSaveResolution.error(forbidden());
    }

    private record PageSaveResolution(CustomPage page, Long blogIdForSlug, Response errorResponse) {
        static PageSaveResolution ok(CustomPage page, Long blogIdForSlug) {
            return new PageSaveResolution(page, blogIdForSlug, null);
        }

        static PageSaveResolution error(Response errorResponse) {
            return new PageSaveResolution(null, null, errorResponse);
        }
    }

    private String validate(CustomPageForm form) {
        if (form.getTitle() == null || form.getTitle().isBlank()) {
            return "Title is required.";
        }
        if (form.getSlug() == null || form.getSlug().isBlank()) {
            return "Slug is required.";
        }
        if (form.getSection() == null || form.getSection().isBlank()) {
            return "Section is required.";
        }
        if (form.getContent() == null || form.getContent().isBlank()) {
            return "Content is required.";
        }
        var pathSlug = CustomPagePaths.pathSlug(form.getSlug().trim());
        if (!pathSlug.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
            return "Slug must contain only letters, numbers, hyphens and underscores.";
        }
        return null;
    }
}
