package dev.vepo.contraponto.readingtime;

import java.time.LocalDateTime;
import java.time.ZoneId;

import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostEngagementService;
import dev.vepo.contraponto.post.PostRepository;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.view.SessionIdProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/forms/posts/{postId}/reading-time")
public class ReadingTimeEndpoint {

    private static final int HEARTBEAT_SECONDS = 5;

    private final PostRepository postRepository;
    private final ReadingTimeRepository readingTimeRepository;
    private final PostEngagementService postEngagementService;
    private final SessionIdProvider sessionIdProvider;
    private final LoggedUser loggedUser;

    @Inject
    public ReadingTimeEndpoint(PostRepository postRepository,
                               ReadingTimeRepository readingTimeRepository,
                               PostEngagementService postEngagementService,
                               SessionIdProvider sessionIdProvider,
                               LoggedUser loggedUser) {
        this.postRepository = postRepository;
        this.readingTimeRepository = readingTimeRepository;
        this.postEngagementService = postEngagementService;
        this.sessionIdProvider = sessionIdProvider;
        this.loggedUser = loggedUser;
    }

    private boolean isReadable(Post post) {
        return post.isPublished() && post.getBlog() != null && post.getBlog().isActive();
    }

    @POST
    @Transactional
    public Response recordReadingTime(@PathParam("postId") long postId, @Context HttpHeaders headers) {
        Post post = postRepository.findById(postId)
                                  .filter(this::isReadable)
                                  .orElseThrow(NotFoundException::new);

        var viewCookie = headers.getCookies().get(SessionIdProvider.VIEW_SESSION_COOKIE);
        String sessionId = sessionIdProvider.getOrCreateSessionId(viewCookie);
        Long userId = loggedUser.isAuthenticated() ? loggedUser.getId() : null;

        if (postEngagementService.shouldRecordReaderEngagement(post, userId)) {
            readingTimeRepository.addSeconds(post,
                                             userId,
                                             sessionId,
                                             HEARTBEAT_SECONDS,
                                             LocalDateTime.now(ZoneId.systemDefault()));
        }

        Response.ResponseBuilder response = Response.noContent();
        if (viewCookie == null) {
            NewCookie cookie = sessionIdProvider.createSessionCookie(sessionId);
            response.cookie(cookie);
        }
        return response.build();
    }
}
