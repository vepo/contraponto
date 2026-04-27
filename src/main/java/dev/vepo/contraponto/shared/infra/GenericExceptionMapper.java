package dev.vepo.contraponto.shared.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.qute.Template;
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

@Provider
@ApplicationScoped
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(GenericExceptionMapper.class);
    private static final boolean IS_DEV = "dev".equals(System.getProperty("quarkus.profile"));

    private final Template error;
    private final LoggedUser loggedUser;

    @Inject
    public GenericExceptionMapper(Template error, LoggedUser loggedUser) {
        this.error = error;
        this.loggedUser = loggedUser;
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

    private String getUserMessage(Throwable exception, int status) {
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
        // Determine HTTP status
        int status = getStatus(exception);
        String userMessage = getUserMessage(exception, status);
        String technicalMessage = IS_DEV ? exception.getMessage() : null;

        // Log the error with stack trace for server-side debugging
        if (status >= 500) {
            logger.error("Internal server error: {}", exception.getMessage(), exception);
        } else {
            logger.warn("Client error {}: {}", status, exception.getMessage());
        }

        // Render error page
        String html = error
                           .data("user", loggedUser)
                           .data("status", status)
                           .data("message", technicalMessage)
                           .data("description", userMessage)
                           .data("dev", IS_DEV)
                           .render();

        return Response.status(status)
                       .entity(html)
                       .type(MediaType.TEXT_HTML)
                       .build();
    }
}