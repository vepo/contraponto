package dev.vepo.contraponto.components.forms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.components.MenuEndpoint;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.UserRepository;
import dev.vepo.contraponto.view.SessionIdProvider;
import dev.vepo.contraponto.view.ViewRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@ApplicationScoped
@Path("/forms/auth/login")
public class LoginEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LoginEndpoint.class);

    // Constants for better maintainability
    public static final String SESSION_COOKIE_NAME = "__session";
    private static final String SESSION_COOKIE_PATH = "/";
    private static final String HX_TRIGGER_HEADER = "HX-Trigger";
    private static final String LOGGED_IN_EVENT = "loggedIn";
    private static final String ERROR_MESSAGE_HTML = "<div class='error-message'>%s</div>";
    private static final String MENU_CONTAINER_ID = "menu-container";
    private static final String MODAL_CLOSE_SCRIPT = """
                                                     <script>
                                                         document.getElementById('authModal').classList.remove('modal--open');
                                                     </script>
                                                     """;

    private final UserRepository userRepository;
    private final ViewRepository viewRepository;
    private final LoggedUserProvider loggedUserProvider;
    private final PasswordService passwordService;

    @Inject
    public LoginEndpoint(UserRepository userRepository,
                         ViewRepository viewRepository,
                         LoggedUserProvider loggedUserProvider,
                         PasswordService passwordService) {
        this.userRepository = userRepository;
        this.viewRepository = viewRepository;
        this.loggedUserProvider = loggedUserProvider;
        this.passwordService = passwordService;
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@FormParam("login") String login,
                          @FormParam("password") String password,
                          @Context HttpHeaders headers) {

        // Input validation
        if (isBlank(login) || isBlank(password)) {
            logger.warn("Login attempt with empty email or password");
            return buildErrorResponse("Login and password are required.");
        }

        return userRepository.findByUsernameOrEmail(login)
                             .map(user -> {
                                 boolean passwordValid = passwordService.verifyPassword(password, user.getPasswordHash());
                                 boolean isActive = user.isActive();

                                 if (!passwordValid || !isActive) {
                                     logger.warn("Login failed for user {}: passwordValid={}, active={}",
                                                 login, passwordValid, isActive);
                                     return buildErrorResponse("Invalid username/email or password.");
                                 }

                                 // get anonymous session ID from cookie before login
                                 var viewCookie = headers.getCookies().get(SessionIdProvider.VIEW_SESSION_COOKIE);
                                 var anonymousSessionId = viewCookie != null ? viewCookie.getValue() : null;

                                 // Successful login
                                 var loggedUser = loggedUserProvider.login(user);
                                 logger.info("User logged in successfully: {}", login);

                                 // migrate anonymous views
                                 if (anonymousSessionId != null && !anonymousSessionId.isBlank()) {
                                     viewRepository.migrateAnonymousViewsToUser(loggedUser.getId(), anonymousSessionId);
                                 }

                                 var menuHtml = MenuEndpoint.Template.menu(loggedUser).render();
                                 var responseBody = buildSuccessResponseBody(menuHtml);

                                 return Response.ok(responseBody)
                                                .header("Set-Cookie", buildSessionCookieHeader(loggedUser.getSessionId()))
                                                .header(HX_TRIGGER_HEADER, LOGGED_IN_EVENT)
                                                .build();
                             })
                             .orElseGet(() -> {
                                 logger.warn("Login failed - user not found: {}", login);
                                 return buildErrorResponse("Invalid username/email or password.");
                             });
    }

    /**
     * Builds the HTML response for a successful login. Uses OOB swap to replace the
     * menu and a script to close the modal.
     */
    private String buildSuccessResponseBody(String menuHtml) {
        return String.format("""
                             <div hx-swap-oob="true" id="%s">%s</div>
                             %s
                             """, MENU_CONTAINER_ID, menuHtml, MODAL_CLOSE_SCRIPT);
    }

    /**
     * Builds the Set-Cookie header value for the session cookie.
     */
    private String buildSessionCookieHeader(String sessionId) {
        return String.format("%s=%s; Path=%s",
                             SESSION_COOKIE_NAME, sessionId, SESSION_COOKIE_PATH);
    }

    /**
     * Returns a consistent error response for authentication failures.
     */
    private Response buildErrorResponse(String errorMessage) {
        return Response.status(Status.BAD_REQUEST)
                       .entity(ERROR_MESSAGE_HTML.formatted(errorMessage))
                       .build();
    }

    /**
     * Utility method to check for blank strings (null, empty, or only whitespace).
     */
    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}