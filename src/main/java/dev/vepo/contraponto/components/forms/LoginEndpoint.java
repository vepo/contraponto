package dev.vepo.contraponto.components.forms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.components.MenuEndpoint;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@Path("/forms/auth/login")
@ApplicationScoped
public class LoginEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginEndpoint.class);

    // Constants for better maintainability
    private static final String SESSION_COOKIE_NAME = "__session";
    private static final String SESSION_COOKIE_PATH = "/";
    private static final String HX_TRIGGER_HEADER = "HX-Trigger";
    private static final String LOGGED_IN_EVENT = "loggedIn";
    private static final String ERROR_MESSAGE_HTML = "<div class='error-message'>Invalid email or password</div>";
    private static final String MENU_CONTAINER_ID = "menu-container";
    private static final String MODAL_CLOSE_SCRIPT = """
                                                     <script>
                                                         document.getElementById('authModal').classList.remove('modal--open');
                                                     </script>
                                                     """;

    private final UserRepository userRepository;
    private final LoggedUserProvider loggedUserProvider;
    private final PasswordService passwordService;

    @Inject
    public LoginEndpoint(UserRepository userRepository,
                         LoggedUserProvider loggedUserProvider,
                         PasswordService passwordService) {
        this.userRepository = userRepository;
        this.loggedUserProvider = loggedUserProvider;
        this.passwordService = passwordService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response login(@FormParam("email") String email,
                          @FormParam("password") String password) {

        // Input validation
        if (isBlank(email) || isBlank(password)) {
            LOGGER.warn("Login attempt with empty email or password");
            return buildErrorResponse();
        }

        return userRepository.findByEmail(email)
                             .map(user -> {
                                 boolean passwordValid = passwordService.verifyPassword(password, user.getPasswordHash());
                                 boolean isActive = user.isActive();

                                 if (!passwordValid || !isActive) {
                                     LOGGER.warn("Login failed for user {}: passwordValid={}, active={}",
                                                 email, passwordValid, isActive);
                                     return buildErrorResponse();
                                 }

                                 // Successful login
                                 var loggedUser = loggedUserProvider.login(user);
                                 LOGGER.info("User logged in successfully: {}", email);

                                 String menuHtml = MenuEndpoint.Template.menu(loggedUser).render();
                                 String responseBody = buildSuccessResponseBody(menuHtml);

                                 return Response.ok(responseBody)
                                                .header("Set-Cookie", buildSessionCookieHeader(loggedUser.getSessionId()))
                                                .header(HX_TRIGGER_HEADER, LOGGED_IN_EVENT)
                                                .build();
                             })
                             .orElseGet(() -> {
                                 LOGGER.warn("Login failed - user not found: {}", email);
                                 return buildErrorResponse();
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
    private Response buildErrorResponse() {
        return Response.status(Status.BAD_REQUEST)
                       .entity(ERROR_MESSAGE_HTML)
                       .build();
    }

    /**
     * Utility method to check for blank strings (null, empty, or only whitespace).
     */
    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
}