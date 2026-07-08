package dev.vepo.contraponto.shared.infra;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.qute.Template;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import dev.vepo.contraponto.custompage.Links;
import dev.vepo.contraponto.seo.SeoService;
import dev.vepo.contraponto.user.LoggedUser;

@Provider
@ApplicationScoped
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Links EMPTY_LINKS = new Links(Map.of());

    private static final Logger logger = LoggerFactory.getLogger(GenericExceptionMapper.class);

    private static String clientIp(ContainerRequestContext context) {
        var forwarded = context.getHeaderString("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var realIp = context.getHeaderString("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return "unknown";
    }

    private final Template error;
    private final LoggedUser loggedUser;
    private final SeoService seoService;
    private final ContainerRequestContext requestContext;

    private final boolean showErrorDetails;

    @Inject
    public GenericExceptionMapper(Template error,
                                  LoggedUser loggedUser,
                                  SeoService seoService,
                                  ContainerRequestContext requestContext,
                                  @ConfigProperty(name = "app.show-error-details", defaultValue = "false") boolean showErrorDetails) {
        this.error = error;
        this.loggedUser = loggedUser;
        this.seoService = seoService;
        this.requestContext = requestContext;
        this.showErrorDetails = showErrorDetails;
    }

    private int getStatus(Throwable exception) {
        if (exception instanceof WebApplicationException webEx) {
            return webEx.getResponse().getStatus();
        }
        if (exception instanceof jakarta.validation.ValidationException) {
            return Status.BAD_REQUEST.getStatusCode();
        }
        if (exception instanceof NotFoundException) {
            return Status.NOT_FOUND.getStatusCode();
        }
        if (exception instanceof ForbiddenException) {
            return Status.FORBIDDEN.getStatusCode();
        }
        return Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    private String getUserMessage(int status) {
        return switch (status) {
            case 400 -> "The request could not be processed due to invalid input.";
            case 403 -> "You are not allowed to access this resource.";
            case 404 -> "The requested page could not be found.";
            case 405 -> "The HTTP method is not supported for this endpoint.";
            case 409 -> "There was a conflict with the current state of the resource.";
            case 410 -> "This resource is no longer available.";
            case 429 -> "Too many requests. Please try again later.";
            default -> "An unexpected error occurred. Please try again later.";
        };
    }

    @Override
    public Response toResponse(Throwable exception) {
        int status = getStatus(exception);
        String userMessage = getUserMessage(status);
        String technicalMessage = showErrorDetails ? exception.getMessage() : null;

        var method = requestContext.getMethod();
        var uri = requestContext.getUriInfo().getRequestUri();
        var clientIp = clientIp(requestContext);
        var userAgent = requestContext.getHeaderString("User-Agent");
        var referer = requestContext.getHeaderString("Referer");
        var host = requestContext.getHeaderString("Host");
        var htmx = requestContext.getHeaderString("HX-Request");

        if (status >= 500) {
            logger.error("Internal server error method={} uri={} clientIp={} host={} userAgent={} referer={} htmx={}",
                         method,
                         uri,
                         clientIp,
                         host,
                         userAgent,
                         referer,
                         htmx,
                         exception);
        } else {
            logger.warn("Client error status={} method={} uri={} clientIp={} host={} userAgent={} referer={} htmx={} exception={}",
                        status,
                        method,
                        uri,
                        clientIp,
                        host,
                        userAgent,
                        referer,
                        htmx,
                        exception);
        }

        String html = error.data("user", loggedUser)
                           .data("status", status)
                           .data("message", technicalMessage)
                           .data("description", userMessage)
                           .data("links", EMPTY_LINKS)
                           .data("dev", showErrorDetails)
                           .data("seo", seoService.forError(status))
                           .render();

        return Response.status(status)
                       .entity(html)
                       .type(MediaType.TEXT_HTML)
                       .build();
    }
}
