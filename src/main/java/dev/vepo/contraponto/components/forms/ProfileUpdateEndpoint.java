package dev.vepo.contraponto.components.forms;

import dev.vepo.contraponto.shared.infra.Logged;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/profile")
public class ProfileUpdateEndpoint {

    private final AccountSecurityUpdateEndpoint accountSecurityUpdateEndpoint;
    private final AuthorAppearanceUpdateEndpoint authorAppearanceUpdateEndpoint;

    @Inject
    public ProfileUpdateEndpoint(AccountSecurityUpdateEndpoint accountSecurityUpdateEndpoint,
                                 AuthorAppearanceUpdateEndpoint authorAppearanceUpdateEndpoint) {
        this.accountSecurityUpdateEndpoint = accountSecurityUpdateEndpoint;
        this.authorAppearanceUpdateEndpoint = authorAppearanceUpdateEndpoint;
    }

    @POST
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response update(@BeanParam ProfileUpdateRequest request) {
        boolean wantsAppearance = request.name() != null
                || request.profilePictureId() != null
                || request.defaultBannerId() != null;
        boolean wantsSecurity = request.email() != null
                || (request.newPassword() != null && !request.newPassword().isBlank());

        if (wantsAppearance && !wantsSecurity) {
            return authorAppearanceUpdateEndpoint.update(new AuthorAppearanceUpdateRequest(request.name(),
                                                                                           request.currentPassword(),
                                                                                           request.profilePictureId(),
                                                                                           request.defaultBannerId()));
        }
        if (wantsSecurity) {
            return accountSecurityUpdateEndpoint.update(new AccountSecurityUpdateRequest(request.email(),
                                                                                         request.currentPassword(),
                                                                                         request.newPassword(),
                                                                                         request.confirmPassword()));
        }
        return authorAppearanceUpdateEndpoint.update(new AuthorAppearanceUpdateRequest(request.name(),
                                                                                       request.currentPassword(),
                                                                                       request.profilePictureId(),
                                                                                       request.defaultBannerId()));
    }
}
