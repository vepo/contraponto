package dev.vepo.contraponto.tag;

import java.util.ArrayList;
import java.util.List;

import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TagProfileService {

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Inject
    public TagProfileService(TagRepository tagRepository, UserRepository userRepository) {
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
    }

    public long countDistinctAuthorsForTag(String tagSlug) {
        return tagRepository.countDistinctAuthorsForTag(tagSlug);
    }

    public List<AuthorTagUsage> mainAuthorsForTag(String tagSlug, int limit) {
        List<AuthorTagUsage> result = new ArrayList<>();
        for (Object[] row : tagRepository.findAuthorUsageRowsForTag(tagSlug, limit)) {
            long userId = ((Number) row[0]).longValue();
            long count = ((Number) row[1]).longValue();
            userRepository.findById(userId).ifPresent(author -> result.add(new AuthorTagUsage(author, count)));
        }
        return result;
    }

    public List<TagUsage> topTagsForAuthor(long ownerId, int limit) {
        return tagRepository.findTopTagUsagesForAuthor(ownerId, limit);
    }

    public List<TagUsage> topTagsForBlog(long blogId, int limit) {
        return tagRepository.findTopTagUsagesForBlog(blogId, limit);
    }
}
