package dev.vepo.contraponto.shared.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/i18n/messages")
public class I18nMessagesEndpoint {

    private final LocalePreference localePreference;

    @Inject
    public I18nMessagesEndpoint(LocalePreference localePreference) {
        this.localePreference = localePreference;
    }

    @GET
    @Path("/{locale}.json")
    public Response messages(@PathParam("locale") String locale) {
        var normalized = localePreference.normalize(locale);
        if (LocalePreference.DEFAULT_LOCALE.equals(normalized)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!"en".equals(normalized) && !"es".equals(normalized)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String resourcePath = "/i18n/messages_" + normalized + ".json";
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            var body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return Response.ok(body)
                           .type("application/json; charset=utf-8")
                           .header("Cache-Control", "public, max-age=3600")
                           .build();
        } catch (IOException e) {
            return Response.serverError().build();
        }
    }
}
