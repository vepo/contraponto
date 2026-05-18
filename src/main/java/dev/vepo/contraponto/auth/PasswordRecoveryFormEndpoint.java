package dev.vepo.contraponto.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/forms/auth/password-recovery")
public class PasswordRecoveryFormEndpoint {

    private final PasswordRecoveryService passwordRecoveryService;

    @Inject
    public PasswordRecoveryFormEndpoint(PasswordRecoveryService passwordRecoveryService) {
        this.passwordRecoveryService = passwordRecoveryService;
    }

    @POST
    @Path("/request")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response request(@FormParam("email") String email) {
        passwordRecoveryService.requestReset(email);
        return Response.ok("""
                           <div class="success-message">If an account exists for that email, we sent reset instructions.</div>
                           """)
                       .header("HX-Redirect", "/password-recovery?success=true")
                       .build();
    }

    @POST
    @Path("/reset")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response reset(@FormParam("token") String token,
                          @FormParam("newPassword") String newPassword,
                          @FormParam("confirmPassword") String confirmPassword) {
        var result = passwordRecoveryService.resetPassword(token, newPassword, confirmPassword);

        if (result.success()) {
            return Response.ok("""
                               <div class="success-message">Your password was updated. Sign in with your new password.</div>
                               <p class="auth-form__switch"><a href="#" hx-get="/auth/modal?mode=login" hx-target="#modal-container">Sign In</a></p>
                               """)
                           .build();
        }

        if (result.invalidToken()) {
            return Response.ok("""
                               <div class="error-message visible">This reset link is invalid or has expired. Request a new one.</div>
                               <p class="auth-form__switch"><a href="/password-recovery">Request a new reset link</a></p>
                               """)
                           .build();
        }

        return Response.ok("<div class=\"error-message visible\">%s</div>".formatted(result.errorMessage()))
                       .build();
    }
}
