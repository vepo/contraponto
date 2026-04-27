package dev.vepo.contraponto.components.forms;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.components.MenuEndpoint;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.User;
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

@Path("/forms/auth/signup")
@ApplicationScoped
public class SignUpEndpoint {

    // Constants (align with LoginEndpoint)
    private static final String SESSION_COOKIE_NAME = "__session";
    private static final String SESSION_COOKIE_PATH = "/";
    private static final String HX_TRIGGER_HEADER = "HX-Trigger";
    private static final String LOGGED_IN_EVENT = "loggedIn";
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
    public SignUpEndpoint(UserRepository userRepository,
                          LoggedUserProvider loggedUserProvider,
                          PasswordService passwordService) {
        this.userRepository = userRepository;
        this.loggedUserProvider = loggedUserProvider;
        this.passwordService = passwordService;
    }

    /**
     * Returns a consistent error response for signup failures.
     */
    private Response buildErrorResponse(String errorMessage) {
        return Response.status(Status.BAD_REQUEST)
                       .entity(String.format("<div class='error-message'>%s</div>", errorMessage))
                       .build();
    }

    /**
     * Builds the Set-Cookie header value for the session cookie.
     */
    private String buildSessionCookieHeader(String sessionId) {
        return String.format("%s=%s; Path=%s; HttpOnly; SameSite=Strict",
                             SESSION_COOKIE_NAME, sessionId, SESSION_COOKIE_PATH);
    }

    /**
     * Builds the HTML response for a successful signup. Uses OOB swap to replace
     * the menu and a script to close the modal.
     */
    private String buildSuccessResponseBody(String menuHtml) {
        return String.format("""
                             <div hx-swap-oob="true" id="%s">%s</div>
                             %s
                             """, MENU_CONTAINER_ID, menuHtml, MODAL_CLOSE_SCRIPT);
    }

    /**
     * Utility method to check for blank strings (null, empty, or only whitespace).
     */
    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response signup(@FormParam("username") String username,
                           @FormParam("name") String name,
                           @FormParam("email") String email,
                           @FormParam("password") String password) {

        // 1. Basic input validation
        if (isBlank(username) || isBlank(name) || isBlank(email) || isBlank(password)) {
            return buildErrorResponse("All fields are required.");
        }

        // 2. Validate email format
        if (!email.contains("@") || !email.contains(".")) {
            return buildErrorResponse("Please enter a valid email address.");
        }

        // 3. Check username length/characters (optional)
        if (username.length() < 3 || username.length() > 20) {
            return buildErrorResponse("Username must be between 3 and 20 characters.");
        }

        // 4. Check uniqueness
        if (userRepository.existsByUsername(username)) {
            return buildErrorResponse("Username already taken.");
        }
        if (userRepository.existsByEmail(email)) {
            return buildErrorResponse("Email already registered.");
        }

        // 5. Hash password and create user
        String hashedPassword = passwordService.hashPassword(password);
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPasswordHash(hashedPassword);
        newUser.setActive(true);

        userRepository.save(newUser);

        // Auto-login
        var loggedUser = loggedUserProvider.login(newUser);
        String menuHtml = MenuEndpoint.Templates.menu(loggedUser).render();
        String responseBody = buildSuccessResponseBody(menuHtml);

        return Response.ok(responseBody)
                       .header("Set-Cookie", buildSessionCookieHeader(loggedUser.getSessionId()))
                       .header(HX_TRIGGER_HEADER, LOGGED_IN_EVENT)
                       .build();
    }
}