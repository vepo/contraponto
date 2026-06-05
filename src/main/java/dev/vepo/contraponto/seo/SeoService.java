package dev.vepo.contraponto.seo;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogPaths;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.custompage.CustomPage;
import dev.vepo.contraponto.custompage.CustomPageCache;
import dev.vepo.contraponto.custompage.CustomPagePaths;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.serie.Serie;
import dev.vepo.contraponto.serie.SeriePaths;
import dev.vepo.contraponto.serie.SerieRepository;
import dev.vepo.contraponto.shared.infra.SiteBranding;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.directory.AuthorProfilePaths;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.tag.AuthorTagUsage;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.tag.TagPaths;
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
        return CrawlerPrivatePaths.isPrivatePath(path);
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
                          .title("Autores · %s".formatted(siteBranding.seoName()))
                          .description("Lista de autores com publicações no %s.".formatted(siteBranding.seoName()))
                          .canonicalUrl(publicSiteUrl.absolute("/authors"))
                          .ogType(SeoOgType.WEBSITE)
                          .build();
    }

    public SeoMetadata forAuthorProfile(User author, Blog mainBlog) {
        return forAuthorProfile(author, mainBlog, BreadcrumbTrail.EMPTY);
    }

    public SeoMetadata forAuthorProfile(User author, Blog mainBlog, BreadcrumbTrail breadcrumb) {
        String description = author.getProfileDescription();
        if (description == null || description.isBlank()) {
            description = mainBlog.getDescription();
        }
        String plain = SeoDescription.toPlainText(description);
        if (plain.isBlank()) {
            plain = "Perfil de %s no %s.".formatted(author.getName(), siteBranding.seoName());
        }
        String profilePath = AuthorProfilePaths.url(author);
        return SeoMetadata.builder()
                          .title("%s · Autores · %s".formatted(author.getName(), siteBranding.seoName()))
                          .description(plain)
                          .canonicalUrl(publicSiteUrl.absolute(profilePath))
                          .ogType(SeoOgType.PROFILE)
                          .jsonLd(structuredData.person(author, profilePath, breadcrumb))
                          .build();
    }

    public SeoMetadata forBlogDirectory() {
        return SeoMetadata.builder()
                          .title("Blogs · %s".formatted(siteBranding.seoName()))
                          .description("Explore todos os blogs ativos no %s.".formatted(siteBranding.seoName()))
                          .canonicalUrl(publicSiteUrl.absolute("/explore/blogs"))
                          .ogType(SeoOgType.WEBSITE)
                          .build();
    }

    public SeoMetadata forBlogHome(User author, Blog blog) {
        return forBlogHome(author, blog, BreadcrumbTrail.EMPTY);
    }

    public SeoMetadata forBlogHome(User author, Blog blog, BreadcrumbTrail breadcrumb) {
        String blogLabel = blog.isMain() ? author.getName() : blog.getName();
        String title = "%s · %s".formatted(blogLabel, siteBranding.seoName());
        String description = SeoDescription.toPlainText(blog.getDescription());
        if (description.isBlank()) {
            description = "Publicações de %s no %s.".formatted(blogLabel, siteBranding.seoName());
        }
        String blogPath = BlogPaths.extractUrl(blog);
        return SeoMetadata.builder()
                          .title(title)
                          .description(description)
                          .canonicalUrl(publicSiteUrl.absolute(blogPath))
                          .ogType(SeoOgType.WEBSITE)
                          .jsonLd(structuredData.webPage(blogLabel, blogPath, description, breadcrumb))
                          .build();
    }

    public SeoMetadata forCustomPage(CustomPage page) {
        return forCustomPage(page, BreadcrumbTrail.EMPTY);
    }

    public SeoMetadata forCustomPage(CustomPage page, BreadcrumbTrail breadcrumb) {
        String description = SeoDescription.toPlainText(page.getTitle());
        String plain = description.isBlank() ? page.getTitle() : description;
        String pagePath = CustomPagePaths.publicUrl(page);
        return SeoMetadata.builder()
                          .title("%s · %s".formatted(page.getTitle(), siteBranding.seoName()))
                          .description(plain)
                          .canonicalUrl(publicSiteUrl.absolute(pagePath))
                          .ogType(SeoOgType.WEBSITE)
                          .jsonLd(structuredData.webPage(page.getTitle(), pagePath, plain, breadcrumb))
                          .build();
    }

    public SeoMetadata forHome() {
        return SeoMetadata.builder()
                          .title(siteBranding.seoName())
                          .description("Descubra artigos em destaque e explore blogs e autores na plataforma %s.".formatted(siteBranding.seoName()))
                          .canonicalUrl(publicSiteUrl.absolute("/"))
                          .ogType(SeoOgType.WEBSITE)
                          .jsonLd(structuredData.webSite())
                          .build();
    }

    public SeoMetadata forPost(PublishedPostView view) {
        return forPost(view, BreadcrumbTrail.EMPTY);
    }

    public SeoMetadata forPost(PublishedPostView view, BreadcrumbTrail breadcrumb) {
        Post post = view.post();
        User author = post.getAuthor();
        String title = "%s · %s · %s".formatted(TemplateExtensions.liveTitle(view), author.getName(), siteBranding.seoName());
        String description = describePost(view);
        String path = PostPaths.extractUrl(post);
        PostPublication live = view.live();
        var builder = SeoMetadata.builder()
                                 .title(title)
                                 .description(description)
                                 .canonicalUrl(publicSiteUrl.absolute(path))
                                 .ogType(SeoOgType.ARTICLE)
                                 .jsonLd(structuredData.blogPosting(view, breadcrumb));
        String cover = TemplateExtensions.coverUrl(post);
        if (cover != null) {
            builder.ogImageUrl(publicSiteUrl.absolute(cover));
        }
        if (live != null && live.getPublishedAt() != null) {
            builder.articlePublishedAt(live.getPublishedAt());
            if (live.getVersion() > 1) {
                builder.articleModifiedAt(live.getPublishedAt());
            }
        } else if (post.getPublishedAt() != null) {
            builder.articlePublishedAt(post.getPublishedAt());
        }
        return builder.build();
    }

    public SeoMetadata forPrivatePage(String pageTitle) {
        return SeoMetadata.builder()
                          .title("%s · %s".formatted(pageTitle, siteBranding.seoName()))
                          .description("")
                          .canonicalUrl(publicSiteUrl.absolute("/"))
                          .ogType(SeoOgType.WEBSITE)
                          .noindex(true)
                          .build();
    }

    public SeoMetadata forSearch(String query) {
        String title = query != null && !query.isBlank()
                                                         ? "Busca: %s · %s".formatted(query, siteBranding.seoName())
                                                         : "Busca · %s".formatted(siteBranding.seoName());
        return SeoMetadata.builder()
                          .title(title)
                          .description("Resultados de busca no %s.".formatted(siteBranding.seoName()))
                          .canonicalUrl(publicSiteUrl.absolute("/search"))
                          .ogType(SeoOgType.WEBSITE)
                          .noindex(true)
                          .build();
    }

    public SeoMetadata forSerie(Serie serie) {
        return forSerie(serie, BreadcrumbTrail.EMPTY);
    }

    public SeoMetadata forSerie(Serie serie, BreadcrumbTrail breadcrumb) {
        String description = "Série %s no %s.".formatted(serie.getTitle(), siteBranding.seoName());
        String seriePath = SeriePaths.extractUrl(serie);
        return SeoMetadata.builder()
                          .title("%s · %s".formatted(serie.getTitle(), siteBranding.seoName()))
                          .description(description)
                          .canonicalUrl(publicSiteUrl.absolute(seriePath))
                          .ogType(SeoOgType.WEBSITE)
                          .jsonLd(structuredData.webPage(serie.getTitle(), seriePath, description, breadcrumb))
                          .build();
    }

    public SeoMetadata forTag(Tag tag) {
        return forTag(tag, List.of());
    }

    public SeoMetadata forTag(Tag tag, List<AuthorTagUsage> mainAuthors) {
        return forTag(tag, mainAuthors, BreadcrumbTrail.EMPTY);
    }

    public SeoMetadata forTag(Tag tag, List<AuthorTagUsage> mainAuthors, BreadcrumbTrail breadcrumb) {
        String description = tag.getDescription() != null && !tag.getDescription().isBlank()
                                                                                             ? SeoDescription.toPlainText(tag.getDescription())
                                                                                             : "Artigos com a tag %s no %s.".formatted(tag.getName(),
                                                                                                                                       siteBranding.seoName());
        String tagPath = TagPaths.url(tag);
        List<String> authorUrls = mainAuthors.stream()
                                             .map(usage -> publicSiteUrl.absolute(AuthorProfilePaths.url(usage.author())))
                                             .toList();
        return SeoMetadata.builder()
                          .title("%s · %s".formatted(tag.getName(), siteBranding.seoName()))
                          .description(description)
                          .canonicalUrl(publicSiteUrl.absolute(tagPath))
                          .ogType(SeoOgType.WEBSITE)
                          .jsonLd(structuredData.collectionPage(tag, tagPath, authorUrls, breadcrumb))
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
            pathOnly = "/%s".formatted(pathOnly);
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
