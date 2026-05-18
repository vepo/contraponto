package dev.vepo.contraponto.components.forms;

import dev.vepo.contraponto.components.MenuEndpoint;
import dev.vepo.contraponto.shared.htmx.HtmxTriggers;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.shared.security.SessionCookieSupport;
import dev.vepo.contraponto.user.Role;
import dev.vepo.contraponto.user.UserService;
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
    public static final String SESSION_COOKIE_NAME = LoginEndpoint.SESSION_COOKIE_NAME;
    private static final String SESSION_COOKIE_PATH = "/";
    private static final String MODAL_CLEAR_OOB =
            "<" + "div" + " id=\"modal-container\" hx-swap-oob=\"innerHTML\"></" + "div" + ">";

    private final LoggedUserProvider loggedUserProvider;
    private final UserService userService;
    private final SessionCookieSupport sessionCookieSupport;

    @Inject
    public SignUpEndpoint(LoggedUserProvider loggedUserProvider,
                          UserService userService,
                          SessionCookieSupport sessionCookieSupport) {
        this.loggedUserProvider = loggedUserProvider;
        this.userService = userService;
        this.sessionCookieSupport = sessionCookieSupport;
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
        return sessionCookieSupport.buildSessionCookie(sessionId);
    }

    /**
     * Builds the HTML response for a successful signup. Uses OOB swap to replace
     * the menu and clear the auth modal.
     */
    private String buildSuccessResponseBody(String menuHtml) {
        return String.format("""
                             <div hx-swap-oob="true" id="%s">%s</div>
                             %s
                             """, HtmxTriggers.MENU_CONTAINER_ID, menuHtml, MODAL_CLEAR_OOB);
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

        if (isBlank(username) || isBlank(name) || isBlank(email) || isBlank(password)) {
            return buildErrorResponse("All fields are required.");
        }

        var validationError = userService.validateNewUser(username, name, email, password);
        if (validationError.isPresent()) {
            return buildErrorResponse(validationError.get());
        }

        var newUser = userService.createUser(username, name, email, password, java.util.Set.of(Role.USER));

        // Auto-login
        var loggedUser = loggedUserProvider.login(newUser);
        String menuHtml = MenuEndpoint.Templates.menu(loggedUser).render();
        String responseBody = buildSuccessResponseBody(menuHtml);

        return Response.ok(responseBody)
                       .header("Set-Cookie", buildSessionCookieHeader(loggedUser.getSessionId()))
                       .header(HtmxTriggers.HEADER_AFTER_SETTLE, HtmxTriggers.LOGGED_IN_ON_BODY)
                       .build();
    }
}