package dev.vepo.contraponto.auth;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.navigation.BreadcrumbService;
import dev.vepo.contraponto.navigation.BreadcrumbTrail;
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

@Path("/account/report-signup")
@ApplicationScoped
public class AccountReportSignupEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance report(Links links,
                                                     boolean reported,
                                                     boolean invalidToken,
                                                     BreadcrumbTrail breadcrumb);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final CustomPageRepository customPageRepository;
    private final AccountActivationService accountActivationService;
    private final BreadcrumbService breadcrumbService;

    @Inject
    public AccountReportSignupEndpoint(CustomPageRepository customPageRepository,
                                       AccountActivationService accountActivationService,
                                       BreadcrumbService breadcrumbService) {
        this.customPageRepository = customPageRepository;
        this.accountActivationService = accountActivationService;
        this.breadcrumbService = breadcrumbService;
    }

    private Response renderInvalidToken() {
        return Response.ok(Templates.report(customPageRepository.loadLinks(),
                                            false,
                                            true,
                                            breadcrumbService.forAccountReportSignup()))
                       .build();
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response report(@QueryParam("token") String token) {
        if (token == null || token.isBlank()) {
            return renderInvalidToken();
        }

        if (accountActivationService.reportUnauthorizedSignup(token)) {
            return Response.ok(Templates.report(customPageRepository.loadLinks(),
                                                true,
                                                false,
                                                breadcrumbService.forAccountReportSignup()))
                           .build();
        }

        return renderInvalidToken();
    }
}
