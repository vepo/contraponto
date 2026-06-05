package dev.vepo.contraponto.auth;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
import dev.vepo.contraponto.shared.security.SessionCookieSupport;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Path("/account/activate")
@ApplicationScoped
public class AccountActivationEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance activate(Links links,
                                                       boolean invalidToken,
                                                       BreadcrumbTrail breadcrumb);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final CustomPageRepository customPageRepository;
    private final AccountActivationService accountActivationService;
    private final SessionCookieSupport sessionCookieSupport;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public AccountActivationEndpoint(CustomPageRepository customPageRepository,
                                     AccountActivationService accountActivationService,
                                     SessionCookieSupport sessionCookieSupport,
                                     BreadcrumbService breadcrumbService) {
        this.customPageRepository = customPageRepository;
        this.accountActivationService = accountActivationService;
        this.sessionCookieSupport = sessionCookieSupport;
        this.breadcrumbService = breadcrumbService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response activate(@QueryParam("token") String token) {
        if (token == null || token.isBlank()) {
            return renderInvalidToken();
        }

        var maybeLoggedUser = accountActivationService.activate(token);
        if (maybeLoggedUser.isEmpty()) {
            return renderInvalidToken();
        }

        var loggedUser = maybeLoggedUser.get();
        return Response.seeOther(UriBuilder.fromPath("/").build())
                       .header("Set-Cookie", sessionCookieSupport.buildSessionCookie(loggedUser.getSessionId()))
                       .build();
    }

    private Response renderInvalidToken() {
        return Response.ok(Templates.activate(customPageRepository.loadLinks(),
                                              true,
                                              breadcrumbService.forAccountActivation()))
                       .build();
    }
}
