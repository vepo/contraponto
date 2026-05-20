package dev.vepo.contraponto.directory;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.contraponto.blog.BlogRepository;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.tag.TagProfileService;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthorDirectoryService {

    private static final int CARD_TAG_LIMIT = 3;

    private final UserRepository userRepository;
    private final BlogRepository blogRepository;
    private final PostRepository postRepository;
    private final TagProfileService tagProfileService;

    @Inject
    public AuthorDirectoryService(UserRepository userRepository,
                                  BlogRepository blogRepository,
                                  PostRepository postRepository,
                                  TagProfileService tagProfileService) {
        this.userRepository = userRepository;
        this.blogRepository = blogRepository;
        this.postRepository = postRepository;
        this.tagProfileService = tagProfileService;
    }

    public List<AuthorDirectoryRow> buildRows() {
        List<AuthorDirectoryRow> rows = new ArrayList<>();
        for (User author : userRepository.findAuthorsWithPublishedPostsForDirectory()) {
            var mainBlog = blogRepository.findMainByOwnerId(author.getId()).orElse(null);
            long postCount = postRepository.countPublishedByAuthor(author.getId());
            var topTags = tagProfileService.topTagsForAuthor(author.getId(), CARD_TAG_LIMIT);
            rows.add(new AuthorDirectoryRow(author,
                                            mainBlog,
                                            postCount,
                                            ProfileExcerpt.forAuthor(author, mainBlog),
                                            topTags));
        }
        return rows;
    }
}
