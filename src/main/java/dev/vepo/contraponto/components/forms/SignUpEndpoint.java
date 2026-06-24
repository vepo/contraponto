package dev.vepo.contraponto.components.forms;

import dev.vepo.contraponto.auth.AccountActivationService;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.security.SessionConstants;
import dev.vepo.contraponto.shared.toast.Toast;
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

    public static final String SESSION_COOKIE_NAME = SessionConstants.SESSION_COOKIE_NAME;
    private static final String MODAL_CLEAR_OOB =
            "<div id=\"modal-container\" hx-swap-oob=\"innerHTML\"></div>";

    private final UserService userService;
    private final AccountActivationService accountActivationService;

    @Inject
    public SignUpEndpoint(UserService userService,
                          AccountActivationService accountActivationService) {
        this.userService = userService;
        this.accountActivationService = accountActivationService;
    }

    private Response buildErrorResponse(String errorMessage) {
        return Response.status(Status.BAD_REQUEST)
                       .entity("<div class='error-message'>%s</div>".formatted(errorMessage))
                       .build();
    }

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
        return validationError.map(this::buildErrorResponse)
                              .orElseGet(() -> {
                                  var newUser = userService.createUser(username, name, email, password, java.util.Set.of(Role.USER), false);
                                  accountActivationService.sendActivationEmail(newUser);

                                  return Toast.ok()
                                              .i18nKey(I18nKeys.AUTH_SIGNUP_ACTIVATION_SENT, I18nDefaults.SIGNUP_ACTIVATION_SENT)
                                              .type(Toast.Type.SUCCESS)
                                              .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                                              .html(MODAL_CLEAR_OOB)
                                              .build();
                              });
    }
}
