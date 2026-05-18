package dev.vepo.contraponto.auth;

import dev.vepo.contraponto.custompage.CustomPageRepository;
import dev.vepo.contraponto.custompage.Links;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/password-recovery")
@ApplicationScoped
public class PasswordRecoveryEndpoint {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance request(Links links, String message, boolean success);

        public static native TemplateInstance reset(Links links, String token, boolean invalidToken, String message, boolean success);

        private Templates() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }
    }

    private final CustomPageRepository customPageRepository;
    private final UserAccountTokenService tokenService;

    @Inject
    public PasswordRecoveryEndpoint(CustomPageRepository customPageRepository,
                                    UserAccountTokenService tokenService) {
        this.customPageRepository = customPageRepository;
        this.tokenService = tokenService;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance request(@QueryParam("message") String message,
                                    @DefaultValue("false") @QueryParam("success") boolean success) {
        return Templates.request(customPageRepository.loadLinks(), message, success);
    }

    @GET
    @Path("/reset")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance reset(@QueryParam("token") String token,
                                  @QueryParam("message") String message,
                                  @DefaultValue("false") @QueryParam("success") boolean success) {
        boolean invalidToken = token == null || token.isBlank()
                || tokenService.findValidToken(token).isEmpty();
        if (success) {
            invalidToken = false;
        }
        return Templates.reset(customPageRepository.loadLinks(), token, invalidToken, message, success);
    }
}
