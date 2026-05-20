package dev.vepo.contraponto.write;

import java.util.List;
import java.util.Optional;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.post.Post;

public record WritePage(Optional<Post> post,
                        String editorContent,
                        List<Blog> blogs,
                        Long selectedBlogId,
                        String initialTagsJson,
                        String initialSerieTitle,
                        boolean hasUnpublishedChanges,
                        Long respondsToPostId,
                        String respondsToTitle,
                        String respondsToUrl) {}
