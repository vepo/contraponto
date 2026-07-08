package dev.vepo.contraponto.activitypub.security;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import dev.vepo.contraponto.activitypub.ActivityPubPaths;
import dev.vepo.contraponto.activitypub.ActivityPubSettings;
import dev.vepo.contraponto.activitypub.actor.ActivityPubActorService;
import dev.vepo.contraponto.activitypub.outbox.ActivityPubOutboxService;
import dev.vepo.contraponto.post.Post;
import dev.vepo.contraponto.post.PostRepository;

/**
 * Serves ActivityStreams JSON on canonical blog/post URLs when the client
 * requests {@code application/activity+json}, without registering duplicate
 * JAX-RS paths that would shadow {@code BlogEndpoint} and {@code PostEndpoint}
 * for HTML browsers. Handles {@code GET} and {@code HEAD} (Fediverse clients
 * often probe actor URLs with HEAD before verifying outbound signatures).
 */
@Blocking
@ApplicationScoped
public class ActivityPubJsonResponder {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Pattern MAIN_POST = Pattern.compile("^/?([^/]+)/post/([^/]+)/?$");

    private static final Pattern USER_ROOT = Pattern.compile("^/?([^/]+)/?$");

    private static boolean acceptsActivityJson(String acceptHeader) {
        if (acceptHeader == null || acceptHeader.isBlank()) {
            return false;
        }
        var lower = acceptHeader.toLowerCase();
        return lower.contains(ActivityPubPaths.ACTIVITY_JSON) || lower.contains(ActivityPubPaths.LD_JSON);
    }

    private static boolean isHead(ContainerRequestContext requestContext) {
        return "HEAD".equalsIgnoreCase(requestContext.getMethod());
    }

    private static Response jsonResponse(Object document, boolean headOnly) {
        try {
            var body = JSON.writeValueAsString(document);
            if (headOnly) {
                return Response.ok()
                               .header(HttpHeaders.CONTENT_TYPE, ActivityPubPaths.ACTIVITY_JSON)
                               .header(HttpHeaders.CONTENT_LENGTH,
                                       Integer.toString(body.getBytes(StandardCharsets.UTF_8).length))
                               .build();
            }
            return Response.ok(body)
                           .header(HttpHeaders.CONTENT_TYPE, ActivityPubPaths.ACTIVITY_JSON)
                           .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to serialize ActivityPub JSON", ex);
        }
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/%s".formatted(path);
    }

    private final ActivityPubSettings settings;
    private final ActivityPubActorService actorService;
    private final ActivityPubOutboxService outboxService;
    private final PostRepository postRepository;

    @Inject
    public ActivityPubJsonResponder(ActivityPubSettings settings,
                                    ActivityPubActorService actorService,
                                    ActivityPubOutboxService outboxService,
                                    PostRepository postRepository) {
        this.settings = settings;
        this.actorService = actorService;
        this.outboxService = outboxService;
        this.postRepository = postRepository;
    }

    /**
     * Builds an ActivityPub JSON response for the request path, or {@code null}
     * when the path is not an actor/post object URL this filter owns.
     */
    public Response respond(ContainerRequestContext requestContext) {
        var path = normalizePath(requestContext.getUriInfo().getPath());
        var headOnly = isHead(requestContext);
        var postMatch = MAIN_POST.matcher(path);
        if (postMatch.matches()) {
            return respondWithPostObject(postMatch.group(1), postMatch.group(2), headOnly);
        }
        var userMatch = USER_ROOT.matcher(path);
        if (userMatch.matches()) {
            return respondWithActor(userMatch.group(1), headOnly);
        }
        return null;
    }

    private Response respondWithActor(String username, boolean headOnly) {
        var actor = actorService.findEnabledByUsername(username).orElse(null);
        if (actor == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return jsonResponse(actorService.buildActorDocument(actor.getUser(), actor), headOnly);
    }

    private Response respondWithPostObject(String username, String slug, boolean headOnly) {
        if (actorService.findEnabledByUsername(username).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var post = postRepository.findMainBlogPost(username, slug)
                                 .filter(Post::isPublished)
                                 .orElse(null);
        if (post == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return jsonResponse(outboxService.buildPostObject(post), headOnly);
    }

    /**
     * Whether this filter should short-circuit the request with ActivityPub JSON
     * (GET/HEAD + activity+json Accept on actor or main-post paths).
     */
    public boolean shouldHandle(ContainerRequestContext requestContext) {
        if (!settings.enabled()) {
            return false;
        }
        var method = requestContext.getMethod();
        // HEAD must succeed for ActivityPub clients (e.g. Mastodon keyId probes);
        // without this, JAX-RS returns 406 Not Acceptable for Accept:
        // application/activity+json.
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }
        if (!acceptsActivityJson(requestContext.getHeaderString(HttpHeaders.ACCEPT))) {
            return false;
        }
        var path = normalizePath(requestContext.getUriInfo().getPath());
        return MAIN_POST.matcher(path).matches() || USER_ROOT.matcher(path).matches();
    }
}
