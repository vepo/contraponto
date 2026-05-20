package dev.vepo.contraponto.directory;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/explore")
@ApplicationScoped
public class BlogDirectoryEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance blogs(java.util.List<Blog> blogs,
                                                    Links links,
                                                    LoggedUser user,
                                                    SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final BlogRepository blogRepository;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final SeoService seoService;

    @Inject
    public BlogDirectoryEndpoint(BlogRepository blogRepository,
                                 CustomPageRepository customPageRepository,
                                 LoggedUser loggedUser,
                                 SeoService seoService) {
        this.blogRepository = blogRepository;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.seoService = seoService;
    }

    @GET
    @Path("blogs")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance blogs() {
        return Templates.blogs(blogRepository.findAllActiveWithOwner(),
                               customPageRepository.loadLinks(),
                               loggedUser,
                               seoService.forBlogDirectory());
    }
}
