package dev.vepo.contraponto.shared.i18n;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/forms/locale")
public class LocaleSwitchEndpoint {

    private final LocalePreference localePreference;
    private final LocaleCookieSupport localeCookieSupport;

    @Inject
    public LocaleSwitchEndpoint(LocalePreference localePreference, LocaleCookieSupport localeCookieSupport) {
        this.localePreference = localePreference;
        this.localeCookieSupport = localeCookieSupport;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response switchLocale(@FormParam("locale") String locale) {
        var normalized = localePreference.normalize(locale);
        return Response.noContent()
                       .cookie(localeCookieSupport.buildLocaleCookie(normalized))
                       .build();
    }
}
