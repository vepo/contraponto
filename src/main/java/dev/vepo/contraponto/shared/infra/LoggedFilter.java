package dev.vepo.contraponto.shared.infra;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vepo.contraponto.components.forms.LoginEndpoint;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.Provider;

@Logged
@Provider
public class LoggedFilter implements ContainerRequestFilter {

    private final LoggedUserProvider loggedUserProvider;

    @Inject
    public LoggedFilter(LoggedUserProvider loggedUserProvider) {
        this.loggedUserProvider = loggedUserProvider;
    }

    private static final Logger logger = LoggerFactory.getLogger(LoggedFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Optional.ofNullable(requestContext.getCookies().get(LoginEndpoint.SESSION_COOKIE_NAME))
                .flatMap(sessionId -> loggedUserProvider.find(sessionId.getValue()))
                .ifPresentOrElse(user -> logger.info("User found! user={}", user),
                                 () -> requestContext.abortWith(Response.seeOther(UriBuilder.fromPath("/")
                                                                                            .build())
                                                                        .build()));

    }

}
