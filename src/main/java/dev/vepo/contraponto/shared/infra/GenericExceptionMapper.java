package dev.vepo.contraponto.shared.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.qute.Template;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import dev.vepo.contraponto.user.LoggedUser;

@Provider
@ApplicationScoped
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(GenericExceptionMapper.class);

    private final Template error;
    private final FooterLinksProvider footerLinksProvider;
    private final LoggedUser loggedUser;
    private final boolean showErrorDetails;

    @Inject
    public GenericExceptionMapper(Template error,
                                  FooterLinksProvider footerLinksProvider,
                                  LoggedUser loggedUser,
                                  @ConfigProperty(name = "app.show-error-details", defaultValue = "false") boolean showErrorDetails) {
        this.error = error;
        this.footerLinksProvider = footerLinksProvider;
        this.loggedUser = loggedUser;
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

        if (status >= 500) {
            logger.error("Internal server error", exception);
        } else {
            logger.warn("Client error status={} exception={}", status, exception);
        }

        String html = error.data("user", loggedUser)
                           .data("status", status)
                           .data("message", technicalMessage)
                           .data("description", userMessage)
                           .data("links", footerLinksProvider.loadGlobalLinks())
                           .data("dev", showErrorDetails)
                           .render();

        return Response.status(status)
                       .entity(html)
                       .type(MediaType.TEXT_HTML)
                       .build();
    }
}
