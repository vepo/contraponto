package dev.vepo.contraponto.auth;

import dev.vepo.contraponto.user.LoggedUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

@Path("/account/verify-email")
@ApplicationScoped
public class EmailVerificationEndpoint {

    private final EmailVerificationService emailVerificationService;
    private final LoggedUser loggedUser;

    @Inject
    public EmailVerificationEndpoint(EmailVerificationService emailVerificationService,
                                     LoggedUser loggedUser) {
        this.emailVerificationService = emailVerificationService;
        this.loggedUser = loggedUser;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response verify(@QueryParam("token") String token) {
        var verified = emailVerificationService.verify(token, loggedUser);
        if (verified.isEmpty()) {
            return Response.seeOther(UriBuilder.fromPath("/account/security")
                                               .queryParam("error", "invalid-token")
                                               .build())
                           .build();
        }

        return Response.seeOther(UriBuilder.fromPath("/account/security")
                                           .queryParam("verified", "true")
                                           .build())
                       .build();
    }
}
