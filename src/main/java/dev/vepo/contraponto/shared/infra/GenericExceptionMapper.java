package dev.vepo.contraponto.shared.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(GenericExceptionMapper.class);

    private final Template error;
    private final LoggedUser loggedUser;

    @Inject
    public GenericExceptionMapper(Template error, LoggedUser loggedUser) {
        this.error = error;
        this.loggedUser = loggedUser;
    }

    @Override
    public Response toResponse(Throwable exception) {
        logger.error("Error!", exception);
        return Response.status(500)
                       .entity(error.data("user", loggedUser)
                                    .data("message", exception.getMessage())
                                    .data("description", exception.getMessage())
                                    .data("status", switch (exception) {
                                        case WebApplicationException ex -> ex.getResponse().getStatus();
                                        default -> 500;
                                    })
                                    .render())
                       .type(MediaType.TEXT_HTML)
                       .build();
    }
}