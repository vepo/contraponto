package dev.vepo.contraponto.directory;

import java.util.List;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.tag.TagUsage;
import dev.vepo.contraponto.user.User;

public record AuthorDirectoryRow(User author,
                                 Blog mainBlog,
                                 long postCount,
                                 String excerpt,
                                 List<TagUsage> topTags) {}
