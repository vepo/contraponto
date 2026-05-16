package dev.vepo.contraponto.git;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostPublication;
import dev.vepo.contraponto.tag.Tag;

final class BlogGitMarkdownMapper {

    static LinkedHashMap<String, Object> buildFrontMatter(Post post, JekyllLayoutConvention c) {
        PostPublication live = post.getLivePublication();
        if (live != null) {
            return buildFrontMatter(live, post, c);
        }
        LinkedHashMap<String, Object> fm = new LinkedHashMap<>();
        if (post.getId() != null) {
            fm.put(JekyllLayoutConvention.FM_POST_ID, post.getId());
        }
        fm.put("slug", post.getSlug());
        fm.put(c.layoutFrontMatterKey(), c.defaultLayoutValue());
        fm.put("title", post.getTitle());

        fm.put("description", post.getDescription() == null ? "" : post.getDescription());

        List<String> labels = post.getTags().stream().map(Tag::getName).sorted(String.CASE_INSENSITIVE_ORDER).distinct().toList();
        fm.put("tags", labels);

        if (post.getSerie() != null) {
            fm.put("serie", post.getSerie().getTitle());
        }

        fm.put("featured", post.isFeatured());
        fm.put("published", post.isPublished());
        if (post.getPublishedAt() != null) {
            fm.put("published_at",
                   post.getPublishedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString());
        }
        fm.put("format", post.getFormat().name());
        return fm;
    }

    static LinkedHashMap<String, Object> buildFrontMatter(PostPublication live, Post post, JekyllLayoutConvention c) {
        LinkedHashMap<String, Object> fm = new LinkedHashMap<>();
        if (post.getId() != null) {
            fm.put(JekyllLayoutConvention.FM_POST_ID, post.getId());
        }
        fm.put("slug", live.getSlug());
        fm.put(c.layoutFrontMatterKey(), c.defaultLayoutValue());
        fm.put("title", live.getTitle());
        fm.put("description", live.getDescription() == null ? "" : live.getDescription());

        List<String> labels = live.getTags().stream().map(Tag::getName).sorted(String.CASE_INSENSITIVE_ORDER).distinct().toList();
        fm.put("tags", labels);

        if (post.getSerie() != null) {
            fm.put("serie", post.getSerie().getTitle());
        }

        fm.put("featured", post.isFeatured());
        fm.put("published", post.isPublished());
        if (live.getPublishedAt() != null) {
            fm.put("published_at",
                   live.getPublishedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime().toString());
        }
        fm.put("format", live.getFormat().name());
        return fm;
    }

    private BlogGitMarkdownMapper() {}
}
