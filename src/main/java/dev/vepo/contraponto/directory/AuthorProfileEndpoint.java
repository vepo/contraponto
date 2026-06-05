package dev.vepo.contraponto.directory;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogDescriptionRenderer;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.seo.SeoMetadata;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.tag.TagProfileService;
import dev.vepo.contraponto.tag.TagUsage;
import dev.vepo.contraponto.user.AuthorSocialUrls;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.RawString;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/authors")
@ApplicationScoped
public class AuthorProfileEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance profile(User author,
                                                      Blog mainBlog,
                                                      RawString renderedBio,
                                                      List<AuthorSocialUrls.SocialLink> socialLinks,
                                                      List<TagUsage> topTags,
                                                      List<Blog> blogs,
                                                      List<Post> recentPosts,
                                                      Links links,
                                                      LoggedUser user,
                                                      BreadcrumbTrail breadcrumb,
                                                      SeoMetadata seo);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private static final int TOP_TAG_LIMIT = 8;

    private static final int RECENT_POST_LIMIT = 6;

    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final TagProfileService tagProfileService;
    private final BlogDescriptionRenderer descriptionRenderer;
    private final CustomPageRepository customPageRepository;
    private final LoggedUser loggedUser;
    private final BreadcrumbService breadcrumbService;
    private final SeoService seoService;

    @Inject
    public AuthorProfileEndpoint(UserRepository userRepository,
                                 BlogRepository blogRepository,
                                 PostRepository postRepository,
                                 TagProfileService tagProfileService,
                                 BlogDescriptionRenderer descriptionRenderer,
                                 CustomPageRepository customPageRepository,
                                 LoggedUser loggedUser,
                                 BreadcrumbService breadcrumbService,
                                 SeoService seoService) {
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
        this.postRepository = postRepository;
        this.tagProfileService = tagProfileService;
        this.descriptionRenderer = descriptionRenderer;
        this.customPageRepository = customPageRepository;
        this.loggedUser = loggedUser;
        this.breadcrumbService = breadcrumbService;
        this.seoService = seoService;
    }

    @GET
    @Path("{username}")
    @Operation(hidden = true)
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance profile(@PathParam("username") String username) {
        User author = userRepository.findPublicAuthorByUsername(username)
                                    .orElseThrow(() -> new NotFoundException("Author not found: %s".formatted(username)));
        Blog mainBlog = blogRepository.findMainByOwnerId(author.getId()).orElseThrow(NotFoundException::new);
        String bioSource = author.getProfileDescription();
        if (bioSource == null || bioSource.isBlank()) {
            bioSource = mainBlog.getDescription();
        }
        RawString renderedBio = new RawString(descriptionRenderer.render(bioSource));
        List<Blog> blogs = blogRepository.findActiveBlogs(author.getId()).stream()
                                         .filter(blog -> postRepository.countPublishedByBlog(blog.getId()) > 0)
                                         .toList();
        List<Post> recentPosts = postRepository.findPublishedFeedByAuthor(author.getId(), RECENT_POST_LIMIT);
        var breadcrumb = breadcrumbService.forAuthorProfile(author);
        return Templates.profile(author,
                                 mainBlog,
                                 renderedBio,
                                 AuthorSocialUrls.visibleLinks(author),
                                 tagProfileService.topTagsForAuthor(author.getId(), TOP_TAG_LIMIT),
                                 blogs,
                                 recentPosts,
                                 customPageRepository.loadLinks(),
                                 loggedUser,
                                 breadcrumb,
                                 seoService.forAuthorProfile(author, mainBlog, breadcrumb));
    }
}
