package dev.vepo.contraponto.directory;

import dev.vepo.contraponto.blog.Blog;
import dev.vepo.contraponto.seo.SeoDescription;
import dev.vepo.contraponto.user.User;

public final class ProfileExcerpt {

    public static String forAuthor(User author, Blog mainBlog) {
        if (author.getProfileDescription() != null && !author.getProfileDescription().isBlank()) {
            return SeoDescription.truncate(SeoDescription.toPlainText(author.getProfileDescription()));
        }
        if (mainBlog != null && mainBlog.getDescription() != null && !mainBlog.getDescription().isBlank()) {
            return SeoDescription.truncate(SeoDescription.toPlainText(mainBlog.getDescription()));
        }
        return "";
    }

    public static String forBlog(Blog blog) {
        if (blog.getDescription() == null || blog.getDescription().isBlank()) {
            return "";
        }
        return SeoDescription.truncate(SeoDescription.toPlainText(blog.getDescription()));
    }

    private ProfileExcerpt() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
