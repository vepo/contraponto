package dev.vepo.contraponto.seo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.navigation.BreadcrumbItem;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPaths;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.shared.infra.SiteBranding;
import dev.vepo.contraponto.post.PostTemplateExtensions;
import dev.vepo.contraponto.shared.qute.SharedTemplateExtensions;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.user.AuthorSocialUrls;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StructuredDataBuilder {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SiteBranding siteBranding;
    private final PublicSiteUrl publicSiteUrl;

    @Inject
    public StructuredDataBuilder(SiteBranding siteBranding, PublicSiteUrl publicSiteUrl) {
        this.siteBranding = siteBranding;
        this.publicSiteUrl = publicSiteUrl;
    }

    public String blogPosting(PublishedPostView view, BreadcrumbTrail breadcrumb) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(blogPostingNode(view));
        breadcrumbListNode(breadcrumb).ifPresent(nodes::add);
        return jsonGraph(nodes);
    }

    private Map<String, Object> blogPostingNode(PublishedPostView view) {
        Post post = view.post();
        PostPublication live = view.live();
        User author = post.getAuthor();
        String title = PostTemplateExtensions.liveTitle(view);
        String description = SeoDescription.toPlainText(PostTemplateExtensions.liveDescription(post));
        String url = publicSiteUrl.absolute(PostPaths.extractUrl(post));
        LocalDateTime publishedAt = live != null && live.getPublishedAt() != null
                                                                                  ? live.getPublishedAt()
                                                                                  : post.getPublishedAt();

        var node = new LinkedHashMap<String, Object>();
        node.put("@type", "BlogPosting");
        node.put("headline", title);
        node.put("description", description);
        node.put("url", url);
        node.put("mainEntityOfPage", url);
        if (publishedAt != null) {
            node.put("datePublished", ISO.format(publishedAt));
        }
        if (live != null && live.getVersion() > 1 && live.getPublishedAt() != null) {
            node.put("dateModified", ISO.format(live.getPublishedAt()));
        }
        node.put("author", Map.of(
                                  "@type", "Person",
                                  "name", author.getName(),
                                  "url", publicSiteUrl.absolute("/%s".formatted(author.getUsername()))));
        String imageUrl = resolveCoverUrl(post, live);
        if (imageUrl != null) {
            node.put("image", imageUrl);
        }
        Blog blog = post.getBlog();
        node.put("publisher", Map.of(
                                     "@type", "Organization",
                                     "name", blog.isMain() ? author.getName() : blog.getName(),
                                     "url", publicSiteUrl.absolute(blog.isMain()
                                                                                 ? "/%s".formatted(author.getUsername())
                                                                                 : "/%s/%s".formatted(author.getUsername(), blog.getSlug()))));
        return node;
    }

    private Optional<Map<String, Object>> breadcrumbListNode(BreadcrumbTrail breadcrumb) {
        if (breadcrumb == null || breadcrumb.isEmpty()) {
            return Optional.empty();
        }
        var listItems = new ArrayList<Map<String, Object>>();
        int position = 1;
        for (BreadcrumbItem item : breadcrumb.items()) {
            var listItem = new LinkedHashMap<String, Object>();
            listItem.put("@type", "ListItem");
            listItem.put("position", position++);
            listItem.put("name", item.label());
            if (!item.isCurrent() && item.href() != null && !item.href().isBlank()) {
                listItem.put("item", publicSiteUrl.absolute(item.href()));
            }
            listItems.add(listItem);
        }
        if (listItems.isEmpty()) {
            return Optional.empty();
        }
        var node = new LinkedHashMap<String, Object>();
        node.put("@type", "BreadcrumbList");
        node.put("itemListElement", listItems);
        return Optional.of(node);
    }

    public String collectionPage(Tag tag, String tagPath, List<String> authorProfileUrls, BreadcrumbTrail breadcrumb) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(collectionPageNode(tag, tagPath, authorProfileUrls));
        breadcrumbListNode(breadcrumb).ifPresent(nodes::add);
        return jsonGraph(nodes);
    }

    private Map<String, Object> collectionPageNode(Tag tag, String tagPath, List<String> authorProfileUrls) {
        var node = new LinkedHashMap<String, Object>();
        node.put("@type", "CollectionPage");
        node.put("name", tag.getName());
        node.put("url", publicSiteUrl.absolute(tagPath));
        if (tag.getDescription() != null && !tag.getDescription().isBlank()) {
            node.put("description", SeoDescription.toPlainText(tag.getDescription()));
        }
        if (!authorProfileUrls.isEmpty()) {
            List<Map<String, String>> mentions = new ArrayList<>();
            for (String url : authorProfileUrls) {
                mentions.add(Map.of("@type", "Person", "url", url));
            }
            node.put("about", mentions);
        }
        return node;
    }

    public String graphWithBreadcrumb(Map<String, Object> primaryNode, BreadcrumbTrail breadcrumb) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(primaryNode);
        breadcrumbListNode(breadcrumb).ifPresent(nodes::add);
        return jsonGraph(nodes);
    }

    public String jsonGraph(List<Map<String, Object>> nodes) {
        if (nodes.size() == 1) {
            var single = new LinkedHashMap<String, Object>(nodes.get(0));
            single.put("@context", "https://schema.org");
            return toJson(single);
        }
        var graph = new LinkedHashMap<String, Object>();
        graph.put("@context", "https://schema.org");
        graph.put("@graph", nodes);
        return toJson(graph);
    }

    public String person(User author, String profilePath, BreadcrumbTrail breadcrumb) {
        var node = new LinkedHashMap<String, Object>();
        node.put("@type", "Person");
        node.put("name", author.getName());
        node.put("url", publicSiteUrl.absolute(profilePath));
        List<String> sameAs = AuthorSocialUrls.sameAs(author);
        if (!sameAs.isEmpty()) {
            node.put("sameAs", sameAs);
        }
        return graphWithBreadcrumb(node, breadcrumb);
    }

    private String resolveCoverUrl(Post post, PostPublication live) {
        if (live != null && live.getCover() != null) {
            return publicSiteUrl.absolute(live.getCover());
        }
        if (post.getCover() != null) {
            return publicSiteUrl.absolute(post.getCover());
        }
        return null;
    }

    private String toJson(Map<String, Object> data) {
        try {
            return JSON.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public String webPage(String name, String pagePath, String description, BreadcrumbTrail breadcrumb) {
        var node = new LinkedHashMap<String, Object>();
        node.put("@type", "WebPage");
        node.put("name", name);
        node.put("url", publicSiteUrl.absolute(pagePath));
        if (description != null && !description.isBlank()) {
            node.put("description", description);
        }
        return graphWithBreadcrumb(node, breadcrumb);
    }

    public String webSite() {
        var website = new LinkedHashMap<String, Object>();
        website.put("@type", "WebSite");
        website.put("name", siteBranding.seoName());
        website.put("url", publicSiteUrl.absolute("/"));
        website.put("publisher", Map.of(
                                        "@type", "Organization",
                                        "name", siteBranding.seoName(),
                                        "url", publicSiteUrl.absolute("/")));
        website.put("potentialAction", Map.of(
                                              "@type", "SearchAction",
                                              "target", Map.of(
                                                               "@type", "EntryPoint",
                                                               "urlTemplate", publicSiteUrl.absolute("/search?q={search_term_string}")),
                                              "query-input", "required name=search_term_string"));
        return jsonGraph(List.of(website));
    }
}
