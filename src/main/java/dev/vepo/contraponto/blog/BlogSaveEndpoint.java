package dev.vepo.contraponto.blog;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.toast.Toast;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
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
@Path("/forms/blogs")
public class BlogSaveEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(BlogSaveEndpoint.class);

    private static String normalizeSlug(String slug) {
        return slug.trim().toLowerCase();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private final BlogRepository blogRepository;
    private final BlogAccess blogAccess;
    private final UserRepository userRepository;

    private final CustomPageRepository customPageRepository;

    private final LoggedUser loggedUser;

    @Inject
    public BlogSaveEndpoint(BlogRepository blogRepository,
                            BlogAccess blogAccess,
                            UserRepository userRepository,
                            CustomPageRepository customPageRepository,
                            LoggedUser loggedUser) {
        this.blogRepository = blogRepository;
        this.blogAccess = blogAccess;
        this.userRepository = userRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
    }

    private Response badRequest(String message) {
        return Toast.response(Response.Status.BAD_REQUEST)
                    .message(message)
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @DELETE
    @Path("{id}")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response deactivate(@PathParam("id") long id) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var blog = blogRepository.findById(id).orElse(null);
        if (blog == null) {
            return notFound();
        }
        if (!blogAccess.canDelete(blog, loggedUser)) {
            return badRequest(blog.isMain() ? "The default blog cannot be removed."
                                            : "You cannot remove this blog.");
        }

        if (!blog.isActive()) {
            return successList();
        }

        var activeCount = blogRepository.countActiveByOwnerId(blog.getOwner().getId());
        if (activeCount <= 1) {
            return badRequest("You must keep at least one active blog.");
        }

        blog.setActive(false);
        blogRepository.save(blog);
        logger.info("Deactivated blog id={}", blog.getId());
        return successList();
    }

    private Response forbidden() {
        return Toast.response(Response.Status.FORBIDDEN)
                    .message("You do not have permission to manage blogs.")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    private List<BlogRow> listRows() {
        var editorView = blogAccess.canListAll(loggedUser);
        var blogs = editorView ? blogRepository.findAllForManagement()
                               : blogRepository.findByOwnerIdForManagement(loggedUser.getId());
        return blogs.stream().map(BlogRow::from).toList();
    }

    private Response notFound() {
        return Toast.response(Response.Status.NOT_FOUND)
                    .message("Blog not found.")
                    .type(Toast.Type.ERROR)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .build();
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(@BeanParam BlogForm form) {
        if (!loggedUser.isAuthenticated()) {
            return forbidden();
        }

        var validationError = validate(form);
        if (validationError != null) {
            return badRequest(validationError);
        }

        Blog blog;
        if (form.getId() != null) {
            blog = blogRepository.findById(form.getId()).orElse(null);
            if (blog == null) {
                return notFound();
            }
            if (!blogAccess.canEdit(blog, loggedUser)) {
                return forbidden();
            }
            var updateError = updateExisting(blog, form);
            if (updateError != null) {
                return badRequest(updateError);
            }
            blogRepository.save(blog);
            logger.info("Updated blog id={} slug={}", blog.getId(), blog.getSlug());
            return successList();
        } else {
            if (!blogAccess.canCreate(loggedUser)) {
                return forbidden();
            }
            var owner = userRepository.findById(loggedUser.getId()).orElse(null);
            if (owner == null) {
                return notFound();
            }
            var slug = normalizeSlug(form.getSlug());
            if (blogRepository.existsSlug(owner.getId(), slug, null)) {
                return badRequest("A blog with this slug already exists.");
            }
            if (slug.equals(owner.getUsername())) {
                return badRequest("Secondary blog slug cannot match your username.");
            }
            blog = new Blog(owner, slug, form.getName().trim(), nullToEmpty(form.getDescription()));
            blog.setActive(form.isActive());
            blogRepository.save(blog);
            logger.info("Created blog id={} slug={}", blog.getId(), blog.getSlug());
            return successList();
        }
    }

    private Response successList() {
        var editorView = blogAccess.canListAll(loggedUser);
        return Toast.ok()
                    .message("Blog saved successfully.")
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .url("/blogs")
                    .page(BlogManageEndpoint.Templates.list(listRows(), editorView, customPageRepository.loadLinks(), loggedUser))
                    .build();
    }

    private String updateExisting(Blog blog, BlogForm form) {
        blog.setName(form.getName().trim());
        blog.setDescription(nullToEmpty(form.getDescription()));

        if (blog.isMain()) {
            blog.setActive(true);
            return null;
        }

        var slug = normalizeSlug(form.getSlug());
        if (slug.equals(blog.getOwner().getUsername())) {
            return "Secondary blog slug cannot match your username.";
        }
        if (blogRepository.existsSlug(blog.getOwner().getId(), slug, blog.getId())) {
            return "A blog with this slug already exists.";
        }
        blog.setSlug(slug);

        if (form.isActive()) {
            blog.setActive(true);
        } else if (blogRepository.countActiveByOwnerId(blog.getOwner().getId()) <= 1) {
            return "You must keep at least one active blog.";
        } else {
            blog.setActive(false);
        }
        return null;
    }

    private String validate(BlogForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            return "Name is required.";
        }
        if (form.getId() == null) {
            if (form.getSlug() == null || form.getSlug().isBlank()) {
                return "Slug is required.";
            }
            var slug = normalizeSlug(form.getSlug());
            if (!slug.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
                return "Slug must contain only letters, numbers, hyphens and underscores.";
            }
            if (CustomPagePaths.isReservedSegment(slug)) {
                return "This slug is reserved.";
            }
        } else {
            var blog = blogRepository.findById(form.getId()).orElse(null);
            if (blog != null && !blog.isMain()) {
                if (form.getSlug() == null || form.getSlug().isBlank()) {
                    return "Slug is required.";
                }
                var slug = normalizeSlug(form.getSlug());
                if (!slug.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
                    return "Slug must contain only letters, numbers, hyphens and underscores.";
                }
                if (CustomPagePaths.isReservedSegment(slug)) {
                    return "This slug is reserved.";
                }
            }
        }
        return null;
    }
}
