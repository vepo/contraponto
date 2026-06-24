package dev.vepo.contraponto.image;

import dev.vepo.contraponto.navigation.NavigationHub;
import dev.vepo.contraponto.navigation.NavigationHubService;
import dev.vepo.contraponto.shared.infra.Logged;
import dev.vepo.contraponto.user.LoggedUser;
import dev.vepo.contraponto.shared.i18n.I18nDefaults;
import dev.vepo.contraponto.shared.i18n.I18nKeys;
import dev.vepo.contraponto.shared.toast.Toast;
import dev.vepo.contraponto.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Logged
@ApplicationScoped
@Path("/forms/images/{uuid}/alt")
public class ImageAltSaveEndpoint {

    private static String normalizeSearch(String searchQuery) {
        return searchQuery == null ? null : searchQuery.trim();
    }

    private final ImageControlService imageControlService;
    private final LoggedUser loggedUser;

    private final NavigationHubService navigationHubService;

    @Inject
    public ImageAltSaveEndpoint(ImageControlService imageControlService,
                                LoggedUser loggedUser,
                                NavigationHubService navigationHubService) {
        this.imageControlService = imageControlService;
        this.loggedUser = loggedUser;
        this.navigationHubService = navigationHubService;
    }

    @PUT
    @Transactional
    @Produces(MediaType.TEXT_HTML)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveAlt(@PathParam("uuid") String uuid,
                            @FormParam("altText") String altText,
                            @FormParam("page") @jakarta.ws.rs.DefaultValue("1") int page,
                            @FormParam("q") String searchQuery,
                            @FormParam("hub") String hub) {
        long ownerId = loggedUser.getId();
        try {
            imageControlService.updateAltText(ownerId, uuid, altText);
        } catch (IllegalArgumentException _) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String extraQuery = ImageControlUrls.extraQuery(searchQuery);
        if ("writing".equals(hub)) {
            return Toast.ok()
                        .i18nKey(I18nKeys.TOAST_IMAGE_UPDATED, I18nDefaults.IMAGE_UPDATED)
                        .type(Toast.Type.SUCCESS)
                        .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                        .page(navigationHubService.shell(NavigationHub.WRITING,
                                                         "images",
                                                         page,
                                                         false,
                                                         null,
                                                         null,
                                                         normalizeSearch(searchQuery)))
                        .build();
        }
        return Toast.ok()
                    .i18nKey(I18nKeys.TOAST_IMAGE_UPDATED, I18nDefaults.IMAGE_UPDATED)
                    .type(Toast.Type.SUCCESS)
                    .duration(Toast.TOAST_DEFAULT_DURATION_MS)
                    .page(navigationHubService.shell(NavigationHub.WRITING,
                                                     "images",
                                                     page,
                                                     false,
                                                     null,
                                                     null,
                                                     normalizeSearch(searchQuery)))
                    .build();
    }
}
