package dev.vepo.contraponto.activitypub;

import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vepo.contraponto.post.PostRepository;

import io.smallrye.common.annotation.Blocking;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

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

    private static Response jsonResponse(Object document) {
        try {
            return Response.ok(JSON.writeValueAsString(document))
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

    public Response respond(ContainerRequestContext requestContext) {
        var path = normalizePath(requestContext.getUriInfo().getPath());
        var postMatch = MAIN_POST.matcher(path);
        if (postMatch.matches()) {
            return respondWithPostObject(postMatch.group(1), postMatch.group(2));
        }
        var userMatch = USER_ROOT.matcher(path);
        if (userMatch.matches()) {
            return respondWithActor(userMatch.group(1));
        }
        return null;
    }

    private Response respondWithActor(String username) {
        var actor = actorService.findEnabledByUsername(username).orElse(null);
        if (actor == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return jsonResponse(actorService.buildActorDocument(actor.getUser(), actor));
    }

    private Response respondWithPostObject(String username, String slug) {
        if (actorService.findEnabledByUsername(username).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var post = postRepository.findMainBlogPost(username, slug)
                                 .filter(p -> p.isPublished())
                                 .orElse(null);
        if (post == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return jsonResponse(outboxService.buildPostObject(post));
    }

    public boolean shouldHandle(ContainerRequestContext requestContext) {
        if (!settings.enabled()) {
            return false;
        }
        var method = requestContext.getMethod();
        // HEAD must succeed for ActivityPub clients (e.g. Mastodon keyId probes);
        // without
        // this, JAX-RS returns 406 Not Acceptable for Accept:
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
