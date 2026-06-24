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
        var blogs = blogRepository.findAllActiveWithOwnerForDirectory();
        if (blogs.isEmpty()) {
            return List.of();
        }
        var blogIds = blogs.stream().map(Blog::getId).toList();
        var postCounts = postRepository.countPublishedByBlogIds(blogIds);
        var blogsWithPosts = blogs.stream()
                                  .filter(blog -> postCounts.getOrDefault(blog.getId(), 0L) > 0)
                                  .toList();
        if (blogsWithPosts.isEmpty()) {
            return List.of();
        }
        var activeBlogIds = blogsWithPosts.stream().map(Blog::getId).toList();
        var topTagsByBlog = tagProfileService.topTagsForBlogs(activeBlogIds, CARD_TAG_LIMIT);
        var rows = new ArrayList<BlogDirectoryRow>();
        for (Blog blog : blogsWithPosts) {
            rows.add(new BlogDirectoryRow(blog,
                                          postCounts.getOrDefault(blog.getId(), 0L),
                                          ProfileExcerpt.forBlog(blog),
                                          topTagsByBlog.getOrDefault(blog.getId(), List.of())));
        }
        return rows;
    }
}
