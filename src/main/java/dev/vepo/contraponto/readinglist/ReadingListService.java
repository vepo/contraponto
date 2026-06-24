package dev.vepo.contraponto.readinglist;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.pagination.Page;
import dev.vepo.contraponto.shared.pagination.PageQuery;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class ReadingListService {

    public record SaveResult(SaveResultType type, ReadingListItem item) {
        static SaveResult alreadySaved(ReadingListItem item) {
            return new SaveResult(SaveResultType.ALREADY_SAVED, item);
        }

        static SaveResult created(ReadingListItem item) {
            return new SaveResult(SaveResultType.CREATED, item);
        }

        static SaveResult requeued(ReadingListItem item) {
            return new SaveResult(SaveResultType.REQUEUED, item);
        }
    }

    public enum SaveResultType {
        CREATED,
        REQUEUED,
        ALREADY_SAVED
    }

    private final ReadingListRepository readingListRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private final int maxItemsPerUser;

    private final int maxSavesPerHour;

    @Inject
    public ReadingListService(ReadingListRepository readingListRepository,
                              PostRepository postRepository,
                              UserRepository userRepository,
                              @ConfigProperty(name = "contraponto.reading-list.max-items-per-user", defaultValue = "500") int maxItemsPerUser,
                              @ConfigProperty(name = "contraponto.reading-list.max-saves-per-hour", defaultValue = "60") int maxSavesPerHour) {
        this.readingListRepository = readingListRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.maxItemsPerUser = maxItemsPerUser;
        this.maxSavesPerHour = maxSavesPerHour;
    }

    public ReadingListActionView buildActionView(Post post, Long userId) {
        if (!post.isPublished()) {
            return ReadingListActionView.hidden(post.getId());
        }
        if (userId == null) {
            return ReadingListActionView.guest(post.getId());
        }
        return readingListRepository.findByUserAndPost(userId, post.getId())
                                    .map(item -> ReadingListActionView.forUser(post.getId(),
                                                                               item.getId(),
                                                                               item.isUnread()
                                                                                               ? ReadingListItemState.UNREAD
                                                                                               : ReadingListItemState.READ))
                                    .orElseGet(() -> ReadingListActionView.forUser(post.getId(),
                                                                                   null,
                                                                                   ReadingListItemState.NOT_SAVED));
    }

    public long countUnread(long userId) {
        return readingListRepository.countUnread(userId);
    }

    private void enforceSaveLimits(long userId) {
        if (readingListRepository.countByUser(userId) >= maxItemsPerUser) {
            throw new BadRequestException("Reading list limit reached.");
        }
        LocalDateTime since = LocalDateTime.now(ZoneId.systemDefault()).minusHours(1);
        if (readingListRepository.countSavesSince(userId, since) >= maxSavesPerHour) {
            throw new BadRequestException("Too many saves. Please try again later.");
        }
    }

    public Page<ReadingListRow> findAllPage(long userId, PageQuery query) {
        return readingListRepository.findAllPage(userId, query);
    }

    public Page<ReadingListRow> findUnreadPage(long userId, PageQuery query) {
        return readingListRepository.findUnreadPage(userId, query);
    }

    private ReadingListItem loadOwnedItem(long itemId, long userId) {
        return readingListRepository.findByIdForUser(itemId, userId).orElseThrow(NotFoundException::new);
    }

    private Post loadSavablePost(long postId) {
        Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
        if (!post.isPublished() || !post.getBlog().isActive()) {
            throw new NotFoundException("Post not found.");
        }
        return post;
    }

    @Transactional
    public ReadingListItem markRead(long itemId, long userId) {
        ReadingListItem item = loadOwnedItem(itemId, userId);
        if (item.isUnread()) {
            item.markRead();
            readingListRepository.save(item);
        }
        return item;
    }

    @Transactional
    public ReadingListItem markUnread(long itemId, long userId) {
        ReadingListItem item = loadOwnedItem(itemId, userId);
        if (!item.isUnread()) {
            item.clearReadMark();
            readingListRepository.save(item);
        }
        return item;
    }

    @Transactional
    public Post remove(long itemId, long userId) {
        ReadingListItem item = loadOwnedItem(itemId, userId);
        Post post = item.getPost();
        readingListRepository.delete(item);
        return post;
    }

    @Transactional
    public SaveResult save(long userId, long postId) {
        Post post = loadSavablePost(postId);
        var existing = readingListRepository.findByUserAndPost(userId, postId);
        if (existing.isPresent()) {
            ReadingListItem item = existing.get();
            if (item.isUnread()) {
                return SaveResult.alreadySaved(item);
            }
            item.requeue();
            readingListRepository.save(item);
            return SaveResult.requeued(item);
        }
        enforceSaveLimits(userId);
        User user = userRepository.findById(userId).orElseThrow(NotFoundException::new);
        ReadingListItem item = new ReadingListItem(user, post);
        readingListRepository.save(item);
        return SaveResult.created(item);
    }
}
