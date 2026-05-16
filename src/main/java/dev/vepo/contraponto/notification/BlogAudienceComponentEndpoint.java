package dev.vepo.contraponto.notification;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
@Path("/components/blogs/{blogId}/audience")
public class BlogAudienceComponentEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance audienceControls(BlogAudienceView audience);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogRepository blogRepository;
    private final BlogAudienceService audienceService;
    private final LoggedUser loggedUser;

    @Inject
    public BlogAudienceComponentEndpoint(BlogRepository blogRepository,
                                         BlogAudienceService audienceService,
                                         LoggedUser loggedUser) {
        this.blogRepository = blogRepository;
        this.audienceService = audienceService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance audience(@PathParam("blogId") long blogId) {
        Blog blog = blogRepository.findById(blogId).filter(Blog::isActive).orElseThrow(NotFoundException::new);
        return Templates.audienceControls(buildView(blog));
    }

    public BlogAudienceView buildView(Blog blog) {
        if (!loggedUser.isAuthenticated()) {
            return new BlogAudienceView(blog.getId(), true, false, false, false);
        }
        long userId = loggedUser.getId();
        boolean isOwner = blog.getOwner().getId().equals(userId);
        if (isOwner) {
            return new BlogAudienceView(blog.getId(), false, true, false, false);
        }
        return new BlogAudienceView(blog.getId(),
                                    true,
                                    true,
                                    audienceService.isFollowing(userId, blog.getId()),
                                    audienceService.isEmailSubscribed(userId, blog.getId()));
    }
}
