package dev.vepo.contraponto.postresponse;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
public class PostResponseService {

    private final PostResponseRepository responseRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Inject
    public PostResponseService(PostResponseRepository responseRepository,
                               PostRepository postRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.responseRepository = responseRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void approve(long responseId, long ownerUserId) {
        PostResponse response = loadForModeration(responseId, ownerUserId);
        response.setLinkBackStatus(PostResponseLinkBackStatus.APPROVED);
        response.setResolvedAt(LocalDateTime.now(ZoneId.systemDefault()));
        responseRepository.save(response);
    }

    @Transactional
    public PostResponse createOnPublish(Post responsePost, long sourcePostId, long responderUserId) {
        Post source = postRepository.findById(sourcePostId).orElseThrow(NotFoundException::new);
        if (!source.isPublished()) {
            throw new BadRequestException("Source post must be published.");
        }
        if (source.getAuthor().getId().equals(responderUserId)) {
            throw new BadRequestException("Cannot respond to your own post.");
        }
        if (responseRepository.findByResponsePostId(responsePost.getId()).isPresent()) {
            throw new BadRequestException("Post response already exists.");
        }
        User responder = userRepository.findById(responderUserId).orElseThrow(NotFoundException::new);

        PostResponse response = new PostResponse();
        response.setSourcePost(source);
        response.setResponsePost(responsePost);
        response.setResponder(responder);
        response.setLinkBackStatus(PostResponseLinkBackStatus.PENDING);
        responseRepository.save(response);
        notificationService.notifyPostResponse(source.getAuthor(), source, responder);
        return response;
    }

    public PostResponse findByResponsePost(long responsePostId) {
        return responseRepository.findByResponsePostId(responsePostId).orElse(null);
    }

    private PostResponse loadForModeration(long responseId, long ownerUserId) {
        PostResponse response = responseRepository.findById(responseId).orElseThrow(NotFoundException::new);
        if (!response.getSourcePost().getAuthor().getId().equals(ownerUserId)) {
            throw new ForbiddenException("Only the source post owner can moderate responses.");
        }
        return response;
    }

    @Transactional
    public void reject(long responseId, long ownerUserId) {
        PostResponse response = loadForModeration(responseId, ownerUserId);
        response.setLinkBackStatus(PostResponseLinkBackStatus.REJECTED);
        response.setResolvedAt(LocalDateTime.now(ZoneId.systemDefault()));
        responseRepository.save(response);
    }

    @Transactional
    public void revoke(long responseId, long ownerUserId) {
        PostResponse response = loadForModeration(responseId, ownerUserId);
        response.setLinkBackStatus(PostResponseLinkBackStatus.REVOKED);
        response.setResolvedAt(LocalDateTime.now(ZoneId.systemDefault()));
        responseRepository.save(response);
    }
}
