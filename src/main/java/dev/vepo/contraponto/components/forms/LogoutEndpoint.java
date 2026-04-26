package dev.vepo.contraponto.components.forms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.components.MenuEndpoint;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.shared.infra.LoggedUser;
import dev.vepo.contraponto.shared.infra.LoggedUserProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@Path("/forms/auth/logout")
@ApplicationScoped
public class LogoutEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(LogoutEndpoint.class);
    private final LoggedUser loggedUser;
    private final LoggedUserProvider loggedUserProvider;

    @Inject
    public LogoutEndpoint(LoggedUserProvider loggedUserProvider, LoggedUser loggedUser) {
        this.loggedUserProvider = loggedUserProvider;
        this.loggedUser = loggedUser;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response login() {
        logger.info("Logout... {}", loggedUser);
        loggedUserProvider.logout(loggedUser);
        // On success, return the refreshed menu component and a script to close the
        // modal
        return Response.ok("""
                           <div hx-swap-oob="true" id="menu-container">%s</div>
                           <script>
                             document.cookie = "__session=; max-age=0; path=/;";
                             document.getElementById('authModal').classList.remove('modal--open');
                           </script>
                           """.formatted(MenuEndpoint.Templates.menu(new LoggedUser()).render()))
                       .header("HX-Trigger", "loggedOut")
                       .build();
    }
}
