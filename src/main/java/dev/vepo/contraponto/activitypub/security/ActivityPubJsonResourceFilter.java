package dev.vepo.contraponto.activitypub.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Serves ActivityStreams JSON on canonical blog/post URLs when the client
 * requests {@code application/activity+json}, without registering duplicate
 * JAX-RS paths that would shadow {@code BlogEndpoint} and {@code PostEndpoint}
 * for HTML browsers.
 */
@ApplicationScoped
public class ActivityPubJsonResourceFilter {

    private final Vertx vertx;
    private final ActivityPubJsonResponder responder;

    @Inject
    public ActivityPubJsonResourceFilter(Vertx vertx, ActivityPubJsonResponder responder) {
        this.vertx = vertx;
        this.responder = responder;
    }

    @ServerRequestFilter(preMatching = true)
    public Uni<Response> serveActivityJson(ContainerRequestContext requestContext) {
        if (!responder.shouldHandle(requestContext)) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom()
                  .completionStage(vertx.executeBlocking(() -> responder.respond(requestContext)).toCompletionStage());
    }
}
