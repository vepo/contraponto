package dev.vepo.contraponto.reddit.account;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.reddit.RedditApiException;
import dev.vepo.contraponto.reddit.RedditSettings;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Full-browser-navigation OAuth round trip — deliberately outside
 * {@code /forms/*}: {@code connect} leaves the app for Reddit's consent screen
 * and {@code callback} lands back as a plain top-level GET, neither of which is
 * an HTMX fragment mutation.
 */
@Logged
@ApplicationScoped
@Path("/writing/reddit")
public class RedditOAuthEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(RedditOAuthEndpoint.class);
    private static final String APPEARANCE_REDIRECT = "/writing/appearance";
    private static final String AUTHORIZE_URL = "https://www.reddit.com/api/v1/authorize";

    private final RedditSettings settings;
    private final RedditOAuthStateService stateService;
    private final RedditAccountLinkService linkService;
    private final UserRepository userRepository;
    private final LoggedUser loggedUser;

    @Inject
    public RedditOAuthEndpoint(RedditSettings settings,
                               RedditOAuthStateService stateService,
                               RedditAccountLinkService linkService,
                               UserRepository userRepository,
                               LoggedUser loggedUser) {
        this.settings = settings;
        this.stateService = stateService;
        this.linkService = linkService;
        this.userRepository = userRepository;
        this.loggedUser = loggedUser;
    }

    @GET
    @Path("callback")
    @Produces(MediaType.TEXT_HTML)
    public Response callback(@QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @QueryParam("error") String error) {
        if (!loggedUser.isAuthenticated()) {
            return redirectToAppearance();
        }
        if (error != null && !error.isBlank()) {
            logger.info("Reddit OAuth callback returned error={} for userId={}", error, loggedUser.getId());
            return redirectToAppearance();
        }
        if (!stateService.verify(loggedUser.getSessionId(), state)) {
            logger.warn("Reddit OAuth callback state mismatch for userId={}", loggedUser.getId());
            return redirectToAppearance();
        }
        if (code == null || code.isBlank()) {
            return redirectToAppearance();
        }

        var user = userRepository.findById(loggedUser.getId()).orElse(null);
        if (user == null) {
            return redirectToAppearance();
        }
        try {
            linkService.link(user, code);
        } catch (RedditApiException ex) {
            logger.warn("Reddit account link failed for userId={}: {}", loggedUser.getId(), ex.getMessage());
        }
        return redirectToAppearance();
    }

    @GET
    @Path("connect")
    @Produces(MediaType.TEXT_HTML)
    public Response connect() {
        if (!loggedUser.isAuthenticated() || !settings.enabled()) {
            return redirectToAppearance();
        }
        var clientId = settings.clientId().orElse(null);
        var redirectUri = settings.redirectUri().orElse(null);
        if (clientId == null || redirectUri == null) {
            return redirectToAppearance();
        }
        var state = stateService.generate(loggedUser.getSessionId());
        var authorizeUri = UriBuilder.fromUri(AUTHORIZE_URL)
                                     .queryParam("client_id", clientId)
                                     .queryParam("response_type", "code")
                                     .queryParam("state", state)
                                     .queryParam("redirect_uri", redirectUri)
                                     .queryParam("duration", "permanent")
                                     .queryParam("scope", "identity submit")
                                     .build();
        return Response.seeOther(authorizeUri).build();
    }

    private Response redirectToAppearance() {
        return Response.seeOther(URI.create(APPEARANCE_REDIRECT)).build();
    }
}
