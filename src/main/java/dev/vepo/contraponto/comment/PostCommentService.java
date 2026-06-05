package dev.vepo.contraponto.comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.vepo.contraponto.notification.NotificationService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.user.User;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class PostCommentService {

    public static final int MAX_BODY_LENGTH = 2000;

    private final PostCommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Inject
    public PostCommentService(PostCommentRepository commentRepository,
                              PostRepository postRepository,
                              UserRepository userRepository,
                              NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private void appendChildren(List<CommentView> result,
                                Map<Long, List<PostComment>> childrenByParent,
                                long parentId,
                                CommentViewerContext viewer,
                                int depth) {
        List<PostComment> children = childrenByParent.getOrDefault(parentId, List.of());
        for (PostComment child : children) {
            result.add(toView(child, viewer, depth));
            appendChildren(result, childrenByParent, child.getId(), viewer, depth + 1);
        }
    }

    private void applyInitialStatus(PostComment comment, Post post, User author) {
        if (post.getAuthor().getId().equals(author.getId())) {
            comment.setStatus(CommentStatus.APPROVED);
            comment.setApprovedAt(LocalDateTime.now());
        } else {
            comment.setStatus(CommentStatus.PENDING);
        }
    }

    @Transactional
    public PostComment approve(long postId, long commentId, long ownerUserId) {
        PostComment comment = loadForModeration(postId, commentId, ownerUserId);
        comment.setStatus(CommentStatus.APPROVED);
        comment.setApprovedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    private List<CommentView> buildNestedViews(List<PostComment> replies, long rootId, CommentViewerContext viewer, int baseDepth) {
        Map<Long, List<PostComment>> childrenByParent = new HashMap<>();
        for (PostComment reply : replies) {
            long parentId = reply.getParent().getId();
            childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(reply);
        }

        List<CommentView> result = new ArrayList<>();
        appendChildren(result, childrenByParent, rootId, viewer, baseDepth);
        return result;
    }

    public List<CommentView> buildReplyViews(long rootId, CommentViewerContext viewer) {
        PostComment root = commentRepository.findById(rootId)
                                            .orElseThrow(NotFoundException::new);
        if (!root.isRoot()) {
            throw new NotFoundException("Comment is not a root comment");
        }
        if (!isVisible(root, viewer)) {
            throw new NotFoundException("Comment not found");
        }

        List<PostComment> replies = commentRepository.findRepliesByRootId(rootId);
        List<PostComment> visible = replies.stream().filter(c -> isVisible(c, viewer)).toList();
        return buildNestedViews(visible, rootId, viewer, 1);
    }

    public List<CommentView> buildRootViews(long postId, CommentViewerContext viewer) {
        return commentRepository.findRootComments(postId).stream()
                                .filter(c -> isVisible(c, viewer))
                                .map(c -> toView(c, viewer, 0))
                                .toList();
    }

    @Transactional
    public PostComment createReply(long postId, long parentId, long authorUserId, String body) {
        Post post = loadPublishedPost(postId);
        PostComment parent = commentRepository.findById(parentId)
                                              .orElseThrow(NotFoundException::new);
        if (parent.getPost().getId() != postId) {
            throw new BadRequestException("Parent comment does not belong to this post.");
        }
        if (parent.getStatus() != CommentStatus.APPROVED) {
            throw new BadRequestException("You can only reply to approved comments.");
        }

        User author = userRepository.findById(authorUserId).orElseThrow(NotFoundException::new);
        String trimmed = validateBody(body);

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setParent(parent);
        comment.setRoot(parent.isRoot() ? parent : parent.getRoot());
        comment.setBody(trimmed);
        applyInitialStatus(comment, post, author);
        commentRepository.save(comment);

        if (comment.getStatus() == CommentStatus.PENDING) {
            notificationService.notifyNewComment(post.getAuthor(), post, comment, author);
        }
        return comment;
    }

    @Transactional
    public PostComment createRootComment(long postId, long authorUserId, String body) {
        Post post = loadPublishedPost(postId);
        User author = userRepository.findById(authorUserId).orElseThrow(NotFoundException::new);
        String trimmed = validateBody(body);

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setBody(trimmed);
        applyInitialStatus(comment, post, author);
        commentRepository.save(comment);

        if (comment.getStatus() == CommentStatus.PENDING) {
            notificationService.notifyNewComment(post.getAuthor(), post, comment, author);
        }
        return comment;
    }

    private int depthForPending(PostComment comment) {
        if (comment.isRoot()) {
            return 0;
        }
        int depth = 1;
        PostComment current = comment.getParent();
        while (current != null && !current.isRoot()) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    public List<CommentView> findPendingViews(long postId, CommentViewerContext viewer) {
        if (!viewer.postOwner()) {
            return List.of();
        }
        return commentRepository.findPendingForPost(postId).stream()
                                .map(c -> toView(c, viewer, depthForPending(c)))
                                .toList();
    }

    private boolean isVisible(PostComment comment, CommentViewerContext viewer) {
        return switch (comment.getStatus()) {
            case APPROVED -> true;
            case PENDING -> viewer.postOwner()
                    || (viewer.userId() != null && viewer.userId().equals(comment.getAuthor().getId()));
            case REJECTED -> false;
        };
    }

    private PostComment loadForModeration(long postId, long commentId, long ownerUserId) {
        PostComment comment = commentRepository.findById(commentId)
                                               .orElseThrow(NotFoundException::new);
        if (comment.getPost().getId() != postId) {
            throw new NotFoundException("Comment not found on this post.");
        }
        if (!comment.getPost().getAuthor().getId().equals(ownerUserId)) {
            throw new ForbiddenException("Only the post owner can moderate comments.");
        }
        return comment;
    }

    private Post loadPublishedPost(long postId) {
        Post post = postRepository.findById(postId).orElseThrow(NotFoundException::new);
        if (!post.isPublished()) {
            throw new NotFoundException("Post not found.");
        }
        return post;
    }

    @Transactional
    public PostComment reject(long postId, long commentId, long ownerUserId) {
        PostComment comment = loadForModeration(postId, commentId, ownerUserId);
        comment.setStatus(CommentStatus.REJECTED);
        comment.setApprovedAt(null);
        return commentRepository.save(comment);
    }

    private CommentView toView(PostComment comment, CommentViewerContext viewer, int depth) {
        long replyCount = comment.isRoot() ? commentRepository.countApprovedReplies(comment.getId()) : 0;
        boolean canModerate = viewer.postOwner() && comment.getStatus() == CommentStatus.PENDING;
        boolean pendingOwn = comment.getStatus() == CommentStatus.PENDING
                && viewer.userId() != null
                && viewer.userId().equals(comment.getAuthor().getId())
                && !viewer.postOwner();
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        return new CommentView(comment.getId(),
                               comment.getPost().getId(),
                               comment.getAuthor().getName(),
                               comment.getAuthor().getUsername(),
                               comment.getBody(),
                               comment.getCreatedAt(),
                               comment.getStatus(),
                               replyCount,
                               canModerate,
                               pendingOwn,
                               depth,
                               parentId);
    }

    private String validateBody(String body) {
        if (body == null) {
            throw new BadRequestException("Comment body is required.");
        }
        String trimmed = body.trim();
        if (trimmed.isEmpty()) {
            throw new BadRequestException("Comment body is required.");
        }
        if (trimmed.length() > MAX_BODY_LENGTH) {
            throw new BadRequestException("Comment must be at most %s characters.".formatted(MAX_BODY_LENGTH));
        }
        return trimmed;
    }
}
