package dev.vepo.contraponto.activitypub.inbox;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import dev.vepo.contraponto.activitypub.actor.ActivityPubAppearanceService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.UserRepository;

@Logged
@ApplicationScoped
@Path("/forms/writing/activitypub/follows")
public class ActivityPubFollowManageEndpoint {

    private final LoggedUser loggedUser;
    private final UserRepository userRepository;
    private final ActivityPubFollowRepository followRepository;
    private final ActivityPubInboxService inboxService;
    private final ActivityPubAppearanceService appearanceService;

    @Inject
    public ActivityPubFollowManageEndpoint(LoggedUser loggedUser,
                                           UserRepository userRepository,
                                           ActivityPubFollowRepository followRepository,
                                           ActivityPubInboxService inboxService,
                                           ActivityPubAppearanceService appearanceService) {
        this.loggedUser = loggedUser;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.inboxService = inboxService;
        this.appearanceService = appearanceService;
    }

    @POST
    @Path("{followId}/accept")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response accept(@PathParam("followId") long followId) {
        return manageFollow(followId, true);
    }

    private Response manageFollow(long followId, boolean accept) {
        if (!loggedUser.isAuthenticated()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        var user = userRepository.findById(loggedUser.getId()).orElseThrow(NotFoundException::new);
        var follow = followRepository.findById(followId).orElseThrow(NotFoundException::new);
        if (follow.getLocalActor().getUser().getId() != user.getId()) {
            return Response.status(Status.FORBIDDEN).build();
        }
        if (accept) {
            inboxService.acceptPendingFollow(followId);
        } else {
            inboxService.rejectPendingFollow(followId);
        }
        var view = appearanceService.buildView(user);
        return Response.ok(ActivityPubFollowRequestsFragment.render(view)).build();
    }

    @POST
    @Path("{followId}/reject")
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    public Response reject(@PathParam("followId") long followId) {
        return manageFollow(followId, false);
    }
}
