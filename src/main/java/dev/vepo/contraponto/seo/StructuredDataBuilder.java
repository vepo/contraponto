package dev.vepo.contraponto.seo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEndpoint;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.post.PublishedPostView;
import dev.vepo.contraponto.shared.infra.TemplateExtensions;
import dev.vepo.contraponto.tag.Tag;
import dev.vepo.contraponto.user.AuthorSocialUrls;
import dev.vepo.contraponto.user.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StructuredDataBuilder {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PublicSiteUrl publicSiteUrl;

    @Inject
    public StructuredDataBuilder(PublicSiteUrl publicSiteUrl) {
        this.publicSiteUrl = publicSiteUrl;
    }

    public String blogPosting(PublishedPostView view) {
        Post post = view.post();
        PostPublication live = view.live();
        User author = post.getAuthor();
        String title = TemplateExtensions.liveTitle(view);
        String description = SeoDescription.toPlainText(TemplateExtensions.liveDescription(post));
        String url = publicSiteUrl.absolute(PostEndpoint.extractUrl(post));
        LocalDateTime publishedAt = live != null && live.getPublishedAt() != null
                                                                                  ? live.getPublishedAt()
                                                                                  : post.getPublishedAt();

        var graph = new LinkedHashMap<String, Object>();
        graph.put("@context", "https://schema.org");
        graph.put("@type", "BlogPosting");
        graph.put("headline", title);
        graph.put("description", description);
        graph.put("url", url);
        graph.put("mainEntityOfPage", url);
        if (publishedAt != null) {
            graph.put("datePublished", ISO.format(publishedAt));
        }
        graph.put("author", Map.of(
                                   "@type", "Person",
                                   "name", author.getName(),
                                   "url", publicSiteUrl.absolute("/" + author.getUsername())));
        String imageUrl = resolveCoverUrl(post, live);
        if (imageUrl != null) {
            graph.put("image", imageUrl);
        }
        Blog blog = post.getBlog();
        graph.put("publisher", Map.of(
                                      "@type", "Organization",
                                      "name", blog.isMain() ? author.getName() : blog.getName(),
                                      "url", publicSiteUrl.absolute(blog.isMain()
                                                                                  ? "/" + author.getUsername()
                                                                                  : "/" + author.getUsername() + "/" + blog.getSlug())));
        return toJson(graph);
    }

    public String person(User author, String profilePath) {
        var graph = new LinkedHashMap<String, Object>();
        graph.put("@context", "https://schema.org");
        graph.put("@type", "Person");
        graph.put("name", author.getName());
        graph.put("url", publicSiteUrl.absolute(profilePath));
        List<String> sameAs = AuthorSocialUrls.sameAs(author);
        if (!sameAs.isEmpty()) {
            graph.put("sameAs", sameAs);
        }
        return toJson(graph);
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

    public String tagPage(Tag tag, String tagPath, List<String> authorProfileUrls) {
        var graph = new LinkedHashMap<String, Object>();
        graph.put("@context", "https://schema.org");
        graph.put("@type", "CollectionPage");
        graph.put("name", tag.getName());
        graph.put("url", publicSiteUrl.absolute(tagPath));
        if (tag.getDescription() != null && !tag.getDescription().isBlank()) {
            graph.put("description", SeoDescription.toPlainText(tag.getDescription()));
        }
        if (!authorProfileUrls.isEmpty()) {
            List<Map<String, String>> mentions = new ArrayList<>();
            for (String url : authorProfileUrls) {
                mentions.add(Map.of("@type", "Person", "url", url));
            }
            graph.put("about", mentions);
        }
        return toJson(graph);
    }

    private String toJson(Map<String, Object> data) {
        try {
            return JSON.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public String webSite() {
        var data = Map.<String, Object>of(
                                          "@context", "https://schema.org",
                                          "@type", "WebSite",
                                          "name", "Contraponto",
                                          "url", publicSiteUrl.absolute("/"));
        return toJson(data);
    }
}
