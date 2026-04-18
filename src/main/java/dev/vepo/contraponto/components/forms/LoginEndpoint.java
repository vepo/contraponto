package dev.vepo.contraponto.components.forms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.auth.PasswordService;
import dev.vepo.contraponto.components.MenuEndpoint;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import dev.vepo.contraponto.user.UserRepository;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/forms/auth/login")
@ApplicationScoped
public class LoginEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LoginEndpoint.class);
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
        var user = userRepository.findByEmail(email).orElse(null);
        if (user == null || !passwordService.verifyPassword(password, user.getPasswordHash()) || !user.isActive()) {
            // Return only the error message inside the modal
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("<div class='error-message'>Invalid email or password</div>")
                           .build();
        }
        var loggedUser = loggedUserProvider.login(user);
        // On success, return the refreshed menu component and a script to close the
        // modal
        String menuHtml = MenuEndpoint.Template.menu(loggedUser).render();
        String response = """
                          <div hx-swap-oob="true" id="menu-container">%s</div>
                          <script>
                            document.cookie = '__session=%s; path=/';
                            document.getElementById('authModal').classList.remove('modal--open');
                          </script>
                          """.formatted(menuHtml, loggedUser.getSessionId());
        return Response.ok(response)
                       .header("HX-Trigger", "loggedIn")
                       .build();
    }
}
