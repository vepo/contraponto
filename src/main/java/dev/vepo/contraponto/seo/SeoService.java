package dev.vepo.contraponto.seo;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogEndpoint;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPage;
import dev.vepo.contraponto.custompage.CustomPageCache;
import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePageEndpoint;
import dev.vepo.contraponto.serie.SerieRepository;
import dev.vepo.contraponto.shared.infra.SiteBranding;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.directory.AuthorProfileEndpoint;
import dev.vepo.contraponto.tag.AuthorTagUsage;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagPageEndpoint;
import dev.vepo.contraponto.tag.TagRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SeoService {

    private static String extractQueryParam(String rawPath, String name) {
        int queryStart = rawPath.indexOf('?');
        if (queryStart < 0) {
            return null;
        }
        String query = rawPath.substring(queryStart + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            if (name.equals(key)) {
                String value = eq >= 0 ? pair.substring(eq + 1) : "";
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static boolean isPrivatePath(String path) {
        return path.startsWith("/write")
                || path.startsWith("/writing")
                || path.startsWith("/reading")
                || path.startsWith("/highlights")
                || path.startsWith("/manage")
                || path.startsWith("/account")
                || path.startsWith("/administration")
                || path.startsWith("/editor")
                || path.startsWith("/forms/")
                || path.startsWith("/api/")
                || path.startsWith("/auth/")
                || path.startsWith("/blogs")
                || path.startsWith("/users")
                || path.startsWith("/library")
                || path.startsWith("/dashboard")
                || path.startsWith("/profile")
                || path.startsWith("/review")
                || path.startsWith("/pages")
                || path.startsWith("/comments")
                || path.startsWith("/notifications")
                || path.startsWith("/subscriptions")
                || path.startsWith("/password-recovery")
                || path.startsWith("/_custom_page");
    }

    private final SiteBranding siteBranding;

    private final PublicSiteUrl publicSiteUrl;
    private final StructuredDataBuilder structuredData;
    private final PostRepository postRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    private final SerieRepository serieRepository;

    private final CustomPageCache customPageCache;

    @Inject
    public SeoService(SiteBranding siteBranding,
                      PublicSiteUrl publicSiteUrl,
                      StructuredDataBuilder structuredData,
                      PostRepository postRepository,
                      BlogRepository blogRepository,
                      UserRepository userRepository,
                      TagRepository tagRepository,
                      SerieRepository serieRepository,
                      CustomPageCache customPageCache) {
        this.siteBranding = siteBranding;
        this.publicSiteUrl = publicSiteUrl;
        this.structuredData = structuredData;
        this.postRepository = postRepository;
        this.blogRepository = blogRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.serieRepository = serieRepository;
        this.customPageCache = customPageCache;
    }

    private String describePost(PublishedPostView view) {
        String description = SeoDescription.toPlainText(TemplateExtensions.liveDescription(view.post()));
        if (!description.isBlank()) {
            return description;
        }
        String content = view.live() != null ? view.live().getContent() : view.post().getContent();
        return SeoDescription.toPlainText(content);
    }

    public SeoMetadata forAuthorDirectory() {
        return SeoMetadata.builder()
                          .title("Autores · " + siteBranding.seoName())
                          .description("Lista de autores com publicações no " + siteBranding.seoName() + ".")
                          .canonicalUrl(publicSiteUrl.absolute("/authors"))
                          .ogType(SeoOgType.WEBSITE)
                          .build();
    }

    public SeoMetadata forAuthorProfile(User author, Blog mainBlog) {
        String description = author.getProfileDescription();
        if (description == null || description.isBlank()) {
            description = mainBlog.getDescription();
        }
        String plain = SeoDescription.toPlainText(description);
        if (plain.isBlank()) {
            plain = "Perfil de " + author.getName() + " no " + siteBranding.seoName() + ".";
        }
        String profilePath = AuthorProfileEndpoint.url(author);
        return SeoMetadata.builder()
                          .title(author.getName() + " · Autores · " + siteBranding.seoName())
                          .description(plain)
                          .canonicalUrl(publicSiteUrl.absolute(profilePath))
                          .ogType(SeoOgType.PROFILE)
                          .jsonLd(structuredData.person(author, profilePath))
                          .build();
    }

    public SeoMetadata forBlogDirectory() {
        return SeoMetadata.builder()
                          .title("Blogs · " + siteBranding.seoName())
                          .description("Explore todos os blogs ativos no " + siteBranding.seoName() + ".")
                          .canonicalUrl(publicSiteUrl.absolute("/explore/blogs"))
                          .ogType(SeoOgType.WEBSITE)
                          .build();
    }

    public SeoMetadata forBlogHome(User author, Blog blog) {
        String blogLabel = blog.isMain() ? author.getName() : blog.getName();
        String title = blogLabel + " · " + siteBranding.seoName();
        String description = SeoDescription.toPlainText(blog.getDescription());
        if (description.isBlank()) {
            description = "Publicações de " + blogLabel + " no " + siteBranding.seoName() + ".";
        }
        return SeoMetadata.builder()
                          .title(title)
                          .description(description)
                          .canonicalUrl(publicSiteUrl.absolute(BlogEndpoint.extractUrl(blog)))
                          .ogType(SeoOgType.WEBSITE)
                          .build();
    }

    public SeoMetadata forCustomPage(CustomPage page) {
        String description = SeoDescription.toPlainText(page.getTitle());
        return SeoMetadata.builder()
                          .title(page.getTitle() + " · " + siteBranding.seoName())
                          .description(description.isBlank() ? page.getTitle() : description)
                          .canonicalUrl(publicSiteUrl.absolute(CustomPagePaths.publicUrl(page)))
                          .ogType(SeoOgType.WEBSITE)
                          .build();
    }

    public SeoMetadata forHome() {
        return SeoMetadata.builder()
                          .title(siteBranding.seoName())
                          .description("Descubra artigos em destaque e explore blogs e autores na plataforma "
                                  + siteBranding.seoName()
                                  + ".")
                          .canonicalUrl(publicSiteUrl.absolute("/"))
                          .ogType(SeoOgType.WEBSITE)
                          .jsonLd(structuredData.webSite())
                          .build();
    }

    public SeoMetadata forPost(PublishedPostView view) {
        Post post = view.post();
        User author = post.getAuthor();
        String title = TemplateExtensions.liveTitle(view) + " · " + author.getName() + " · " + siteBranding.seoName();
        String description = describePost(view);
        String path = PostEndpoint.extractUrl(post);
        PostPublication live = view.live();
        var builder = SeoMetadata.builder()
                                 .title(title)
                                 .description(description)
                                 .canonicalUrl(publicSiteUrl.absolute(path))
                                 .ogType(SeoOgType.ARTICLE)
                                 .jsonLd(structuredData.blogPosting(view));
        String cover = TemplateExtensions.coverUrl(post);
        if (cover != null) {
            builder.ogImageUrl(publicSiteUrl.absolute(cover));
        }
        if (live != null && live.getPublishedAt() != null) {
            builder.articlePublishedAt(live.getPublishedAt());
        } else if (post.getPublishedAt() != null) {
            builder.articlePublishedAt(post.getPublishedAt());
        }
        return builder.build();
    }

    public SeoMetadata forPrivatePage(String pageTitle) {
        return SeoMetadata.builder()
                          .title(pageTitle + " · " + siteBranding.seoName())
                          .description("")
                          .canonicalUrl(publicSiteUrl.absolute("/"))
                          .ogType(SeoOgType.WEBSITE)
                          .noindex(true)
                          .build();
    }

    public SeoMetadata forSearch(String query) {
        String title = query != null && !query.isBlank()
                                                         ? "Busca: " + query + " · " + siteBranding.seoName()
                                                         : "Busca · " + siteBranding.seoName();
        return SeoMetadata.builder()
                          .title(title)
                          .description("Resultados de busca no " + siteBranding.seoName() + ".")
                          .canonicalUrl(publicSiteUrl.absolute("/search"))
                          .ogType(SeoOgType.WEBSITE)
                          .noindex(true)
                          .build();
    }

    public SeoMetadata forSerie(Serie serie) {
        String description = "Série " + serie.getTitle() + " no " + siteBranding.seoName() + ".";
        return SeoMetadata.builder()
                          .title(serie.getTitle() + " · " + siteBranding.seoName())
                          .description(description)
                          .canonicalUrl(publicSiteUrl.absolute(SeriePageEndpoint.extractUrl(serie)))
                          .ogType(SeoOgType.WEBSITE)
                          .build();
    }

    public SeoMetadata forTag(Tag tag) {
        return forTag(tag, List.of());
    }

    public SeoMetadata forTag(Tag tag, List<AuthorTagUsage> mainAuthors) {
        String description = tag.getDescription() != null && !tag.getDescription().isBlank()
                                                                                             ? SeoDescription.toPlainText(tag.getDescription())
                                                                                             : "Artigos com a tag " + tag.getName() + " no "
                                                                                                     + siteBranding.seoName() + ".";
        String tagPath = TagPageEndpoint.url(tag);
        List<String> authorUrls = mainAuthors.stream()
                                             .map(usage -> publicSiteUrl.absolute(AuthorProfileEndpoint.url(usage.author())))
                                             .toList();
        return SeoMetadata.builder()
                          .title(tag.getName() + " · " + siteBranding.seoName())
                          .description(description)
                          .canonicalUrl(publicSiteUrl.absolute(tagPath))
                          .ogType(SeoOgType.WEBSITE)
                          .jsonLd(structuredData.tagPage(tag, tagPath, authorUrls))
                          .build();
    }

    private String privateTitle(String path) {
        if (path.startsWith("/write")) {
            return "Escrever";
        }
        if (path.startsWith("/writing")) {
            return "Escrita";
        }
        if (path.startsWith("/reading") || path.startsWith("/highlights")) {
            return "Reading";
        }
        if (path.startsWith("/manage")) {
            return "Gerenciar";
        }
        if (path.startsWith("/account")) {
            return "Conta";
        }
        if (path.startsWith("/search")) {
            return "Busca";
        }
        return siteBranding.seoName();
    }

    private SeoMetadata resolveAuthorBlogHome(String username) {
        return userRepository.findByUsername(username)
                             .flatMap(user -> blogRepository.findMainByOwnerId(user.getId())
                                                            .map(blog -> forBlogHome(user, blog)))
                             .orElseGet(() -> forPrivatePage(username));
    }

    private SeoMetadata resolveBlogCustomPage(String username, String blogSlug, String slug) {
        return customPageCache.findByUsernameBlogSlugAndSlug(username, blogSlug, slug)
                              .map(this::forCustomPage)
                              .orElseGet(() -> forPrivatePage(slug));
    }

    public SeoMetadata resolveFromPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank() || "/".equals(rawPath.trim())) {
            return forHome();
        }
        String pathOnly = rawPath;
        int queryStart = pathOnly.indexOf('?');
        if (queryStart >= 0) {
            pathOnly = pathOnly.substring(0, queryStart);
        }
        if (!pathOnly.startsWith("/")) {
            pathOnly = "/" + pathOnly;
        }

        if ("/authors".equals(pathOnly)) {
            return forAuthorDirectory();
        }
        if (pathOnly.startsWith("/authors/")) {
            String username = pathOnly.substring("/authors/".length());
            if (username.contains("/")) {
                username = username.substring(0, username.indexOf('/'));
            }
            return userRepository.findPublicAuthorByUsername(username)
                                 .flatMap(user -> blogRepository.findMainByOwnerId(user.getId())
                                                                .map(blog -> forAuthorProfile(user, blog)))
                                 .orElseGet(() -> forPrivatePage("Autor"));
        }
        if ("/explore/blogs".equals(pathOnly)) {
            return forBlogDirectory();
        }
        if (pathOnly.equals("/search") || pathOnly.startsWith("/search/")) {
            return forSearch(extractQueryParam(rawPath, "q"));
        }
        if (pathOnly.startsWith("/tags/")) {
            return resolveTag(pathOnly);
        }
        if (pathOnly.startsWith("/page/")) {
            return resolveGlobalCustomPage(pathOnly);
        }
        if (isPrivatePath(pathOnly)) {
            return forPrivatePage(privateTitle(pathOnly));
        }

        List<String> segments = Arrays.stream(pathOnly.split("/"))
                                      .filter(segment -> !segment.isBlank())
                                      .toList();
        if (segments.isEmpty()) {
            return forHome();
        }

        String username = segments.get(0);
        if (CustomPagePaths.isReservedSegment(username)) {
            return forPrivatePage(siteBranding.seoName());
        }

        if (segments.size() == 1) {
            return resolveAuthorBlogHome(username);
        }

        if ("post".equals(segments.get(1)) && segments.size() >= 3) {
            return resolveMainBlogPost(username, segments.get(2));
        }
        if ("serie".equals(segments.get(1)) && segments.size() >= 3) {
            return resolveMainBlogSerie(username, segments.get(2));
        }
        if ("page".equals(segments.get(1)) && segments.size() >= 3) {
            return resolveUserCustomPage(username, segments.get(2));
        }

        if (segments.size() >= 3 && "post".equals(segments.get(2))) {
            return resolveSecondaryBlogPost(username, segments.get(1), segments.get(3));
        }
        if (segments.size() >= 3 && "serie".equals(segments.get(2))) {
            return resolveSecondaryBlogSerie(username, segments.get(1), segments.get(3));
        }
        if (segments.size() >= 3 && "page".equals(segments.get(2))) {
            return resolveBlogCustomPage(username, segments.get(1), segments.get(3));
        }

        if (segments.size() == 2 && !CustomPagePaths.isReservedSegment(segments.get(1))) {
            return resolveSecondaryBlogHome(username, segments.get(1));
        }

        return forPrivatePage(siteBranding.seoName());
    }

    private SeoMetadata resolveGlobalCustomPage(String pathOnly) {
        String slug = pathOnly.substring("/page/".length());
        return customPageCache.findGlobalBySlug(slug)
                              .map(this::forCustomPage)
                              .orElseGet(() -> forPrivatePage("Página"));
    }

    private SeoMetadata resolveMainBlogPost(String username, String slug) {
        return postRepository.findMainBlogPost(username, slug)
                             .map(post -> forPost(new PublishedPostView(post, post.getLivePublication())))
                             .orElseGet(() -> forPrivatePage(slug));
    }

    private SeoMetadata resolveMainBlogSerie(String username, String serieSlug) {
        return serieRepository.findMainBlogSerie(username, serieSlug)
                              .map(this::forSerie)
                              .orElseGet(() -> forPrivatePage(serieSlug));
    }

    private SeoMetadata resolveSecondaryBlogHome(String username, String blogSlug) {
        return blogRepository.findActiveByOwnerUsernameAndSlug(username, blogSlug)
                             .map(blog -> forBlogHome(blog.getOwner(), blog))
                             .orElseGet(() -> forPrivatePage(blogSlug));
    }

    private SeoMetadata resolveSecondaryBlogPost(String username, String blogSlug, String slug) {
        return postRepository.findBlogPost(username, blogSlug, slug)
                             .map(post -> forPost(new PublishedPostView(post, post.getLivePublication())))
                             .orElseGet(() -> forPrivatePage(slug));
    }

    private SeoMetadata resolveSecondaryBlogSerie(String username, String blogSlug, String serieSlug) {
        return serieRepository.findSecondaryBlogSerie(username, blogSlug, serieSlug)
                              .map(this::forSerie)
                              .orElseGet(() -> forPrivatePage(serieSlug));
    }

    private SeoMetadata resolveTag(String pathOnly) {
        String slug = pathOnly.substring("/tags/".length());
        if (slug.contains("/")) {
            slug = slug.substring(0, slug.indexOf('/'));
        }
        return tagRepository.findBySlug(slug)
                            .map(this::forTag)
                            .orElseGet(() -> forPrivatePage("Tag"));
    }

    private SeoMetadata resolveUserCustomPage(String username, String slug) {
        return customPageCache.findByUsernameAndSlug(username, slug)
                              .map(this::forCustomPage)
                              .orElseGet(() -> forPrivatePage(slug));
    }
}
