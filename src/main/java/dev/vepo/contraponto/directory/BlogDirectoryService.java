package dev.vepo.contraponto.directory;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.tag.TagProfileService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BlogDirectoryService {

    private static final int CARD_TAG_LIMIT = 3;

    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final TagProfileService tagProfileService;

    @Inject
    public BlogDirectoryService(BlogRepository blogRepository,
                                PostRepository postRepository,
                                TagProfileService tagProfileService) {
        this.blogRepository = blogRepository;
        this.postRepository = postRepository;
        this.tagProfileService = tagProfileService;
    }

    public List<BlogDirectoryRow> buildRows() {
        List<BlogDirectoryRow> rows = new ArrayList<>();
        for (Blog blog : blogRepository.findAllActiveWithOwner()) {
            if (!hasPublishedPosts(blog)) {
                continue;
            }
            long postCount = postRepository.countPublishedByBlog(blog.getId());
            var topTags = tagProfileService.topTagsForBlog(blog.getId(), CARD_TAG_LIMIT);
            rows.add(new BlogDirectoryRow(blog,
                                          postCount,
                                          ProfileExcerpt.forBlog(blog),
                                          topTags));
        }
        return rows;
    }

    private boolean hasPublishedPosts(Blog blog) {
        return postRepository.countPublishedByBlog(blog.getId()) > 0;
    }
}
